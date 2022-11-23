package com.xi.commons.cache.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.lazada.zal.commons.async.AsyncThreadPool;
import com.lazada.zal.commons.async.NamedThreadFactory;
import com.xi.commons.cache.CacheConfig;
import com.xi.commons.cache.CacheItem;
import com.xi.commons.cache.IBulkCacheLoader;
import com.xi.commons.cache.ICacheLoader;
import com.xi.commons.cache.ICacheManager;
import com.xi.commons.cache.impl.CacheKeyGenerator.DefaultCacheKeyGenerator;

/**
 * 缓存加载管理，确保并发访问同样的 key 时，单机内只有一个线程调用 loader，避免缓存击穿
 */
@SuppressWarnings("unchecked")
class CacheLoaderManager {
    private final ICacheManager cacheManager;
    private final CacheKeyGenerator keyGenerator;
    private final Map<String, CompletableFuture<?>> futures;
    private final ExecutorService executor;

    CacheLoaderManager(ICacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.keyGenerator = new DefaultCacheKeyGenerator(true, true, true);
        this.futures = new ConcurrentHashMap<>();
        this.executor = AsyncThreadPool.newFixedThreadPool(4, new NamedThreadFactory("CacheLoader-", true));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                executor.shutdown();

                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ignored) {
                // ignored...
            }
        }));
    }

    /**
     * 同步加载缓存值
     */
    <K, V> V loadValue(CacheConfig config, K key, ICacheLoader<K, V> loader) {
        if (loader == null) {
            return null;
        }

        // 尝试抢占加载权，如果成功，则执行加载逻辑，如果失败，说明其他线程正在加载，等它加载完成后直接用它的结果即可
        Future<V> existingFuture = tryStartLoading(config, key);
        if (existingFuture == null) {
            return doLoadValue(config, key, loader);
        } else {
            return waitAndGet(existingFuture);
        }
    }

    /**
     * 异步重新加载缓存值
     */
    <K, V> void reloadValueAsync(CacheConfig config, K key, CacheItem<V> item, ICacheLoader<K, V> loader) {
        if (loader == null) {
            return;
        }

        // 如果未开启重加载，或者时间未到，跳过
        if (item.getReloadTime() == 0 || System.currentTimeMillis() < item.getReloadTime()) {
            return;
        }

        // 尝试抢占加载权，如果成功，则执行加载逻辑，如果失败，说明其他线程正在加载，则放弃
        Future<V> existingFuture = tryStartLoading(config, key);
        if (existingFuture == null) {
            try {
                executor.execute(() -> doLoadValue(config, key, loader));
            } catch (Throwable e) {
                completeLoadingExceptionally(config, key, e);
                throw e;
            }
        }
    }

    /**
     * 执行加载逻辑
     */
    private <K, V> V doLoadValue(CacheConfig config, K key, ICacheLoader<K, V> loader) {
        try {
            // 调用 loader 加载
            V value = loader.load(key);

            // 判断是否缓存加载值，如果不需要缓存 null 等特殊值，调用方可通过覆盖 shouldCache 方法实现，默认缓存所有值
            if (loader.shouldCache(value)) {
                cacheManager.put(config.getName(), key, value);
            }

            // 标记为成功完成
            completeLoading(config, key, value);
            return value;

        } catch (Throwable e) {
            // 标记为异常完成
            completeLoadingExceptionally(config, key, e);
            throw e;
        }
    }

    /**
     * 同步批量加载缓存值
     */
    <K, V> Map<K, V> bulkLoadValues(CacheConfig config, Set<K> keys, IBulkCacheLoader<K, V> loader) {
        if (loader == null) {
            return Collections.emptyMap();
        }

        // 批量尝试抢占加载权，如果成功，则执行加载逻辑，如果失败，说明其他线程正在加载，等它加载完成后直接用它的结果即可
        Set<K> loadingKeys = new HashSet<>();
        Map<K, Future<V>> existingFutures = new HashMap<>();

        for (K key : keys) {
            Future<V> future = tryStartLoading(config, key);
            if (future == null) {
                loadingKeys.add(key);
            } else {
                existingFutures.put(key, future);
            }
        }

        // 针对需要加载的 key，调用 loader 加载
        Map<K, V> results = new HashMap<>();
        if (!loadingKeys.isEmpty()) {
            results.putAll(doBulkLoadValues(config, loadingKeys, loader));
        }

        // 针对不需要加载的 key，等待它们完成并获取结果
        for (Entry<K, Future<V>> entry : existingFutures.entrySet()) {
            results.put(entry.getKey(), waitAndGet(entry.getValue()));
        }

        return results;
    }

    /**
     * 异步批量重新加载缓存值
     */
    <K, V> void bulkReloadValuesAsync(CacheConfig config, Map<K, CacheItem<V>> items, IBulkCacheLoader<K, V> loader) {
        if (loader == null) {
            return;
        }

        Set<K> loadingKeys = new HashSet<>();
        for (Entry<K, CacheItem<V>> entry : items.entrySet()) {
            // 如果未开启重加载，或者时间未到，跳过
            CacheItem<V> item = entry.getValue();
            if (item.getReloadTime() == 0 || System.currentTimeMillis() < item.getReloadTime()) {
                continue;
            }

            // 尝试抢占加载权，如果成功，则执行加载逻辑，如果失败，说明其他线程正在加载，则跳过
            Future<V> future = tryStartLoading(config, entry.getKey());
            if (future == null) {
                loadingKeys.add(entry.getKey());
            }
        }

        // 针对符合条件而且抢占成功的 key，提交异步加载任务
        if (!loadingKeys.isEmpty()) {
            try {
                executor.execute(() -> doBulkLoadValues(config, loadingKeys, loader));
            } catch (Throwable e) {
                for (K key : loadingKeys) {
                    completeLoadingExceptionally(config, key, e);
                }

                throw e;
            }
        }
    }

    /**
     * 执行批量加载逻辑
     */
    private <K, V> Map<K, V> doBulkLoadValues(CacheConfig config, Set<K> keys, IBulkCacheLoader<K, V> loader) {
        try {
            // 调用 loader 加载
            Map<K, V> values = loader.load(keys);

            // 判断是否缓存加载值，如果不需要缓存 null 等特殊值，调用方可通过覆盖 shouldCache 方法实现，默认缓存所有值
            Map<K, V> cachedValues = new HashMap<>();
            for (K key : keys) {
                V value = values.get(key);
                if (loader.shouldCache(value)) {
                    cachedValues.put(key, value);
                }
            }

            // 回写数据到缓存
            if (!cachedValues.isEmpty()) {
                cacheManager.putAll(config.getName(), cachedValues);
            }

            // 批量标记为成功完成
            for (K key : keys) {
                completeLoading(config, key, values.get(key));
            }

            return values;

        } catch (Throwable e) {
            // 批量标记为异常完成
            for (K key : keys) {
                completeLoadingExceptionally(config, key, e);
            }

            throw e;
        }
    }

    /**
     * 尝试抢占加载权
     *
     * 若抢占成功，返回 null，允许当前线程执行加载逻辑
     * 若抢占失败，说明有其他线程正在加载当前值，返回该线程的 future，可通过 future 等待获取对应的结果，但当前线程不允许执行加载
     */
    private <K, V> Future<V> tryStartLoading(CacheConfig config, K key) {
        return (Future<V>) futures.putIfAbsent(keyGenerator.generateFullKey(config, key), new CompletableFuture<>());
    }

    /**
     * 标记缓存加载任务成功完成
     */
    private <K, V> void completeLoading(CacheConfig config, K key, V value) {
        CompletableFuture<V> future = (CompletableFuture<V>) futures.remove(keyGenerator.generateFullKey(config, key));
        if (future != null) {
            future.complete(value);
        }
    }

    /**
     * 标记缓存加载任务异常完成
     */
    private <K, V> void completeLoadingExceptionally(CacheConfig config, K key, Throwable e) {
        CompletableFuture<?> future = futures.remove(keyGenerator.generateFullKey(config, key));
        if (future != null) {
            future.completeExceptionally(e);
        }
    }

    /**
     * 阻塞等待 future 完成，并获取返回结果
     */
    private <V> V waitAndGet(Future<V> future) {
        try {
            return future.get(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CacheException("Thread interrupted when waiting for the loader. ", e);
        } catch (ExecutionException e) {
            throw new CacheException("Error occurred in the cache loader. ", e.getCause());
        } catch (TimeoutException e) {
            throw new CacheException("Timed out waiting for the cache loader. ", e);
        }
    }
}
