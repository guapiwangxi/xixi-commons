package com.xi.commons.cache.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.xi.commons.cache.CacheConfig;
import com.xi.commons.cache.CacheItem;
import com.xi.commons.cache.CacheType;
import com.xi.commons.cache.ICacheClient;
import com.xi.commons.cache.impl.CacheKeyGenerator;
import com.xi.commons.cache.monitor.CacheOperationType;
import com.xi.commons.cache.monitor.CacheStatLogger;

/**
 * 抽象缓存客户端，这个抽象类提供了 key 拼接、统计日志等通用功能，具体的缓存客户端继承这个类，实现 do 前缀的方法即可
 */
@SuppressWarnings("unchecked")
public abstract class AbstractCacheClient implements ICacheClient {
    private final CacheType cacheType;
    private final CacheKeyGenerator keyGenerator;
    private final AtomicReference<Map<String, CacheConfig>> cacheConfigs;
    private final CacheStatLogger statLogger;

    /**
     * 构造缓存客户端，提供 cacheType 和 CacheKeyGenerator 的实现
     */
    protected AbstractCacheClient(CacheType cacheType, CacheKeyGenerator keyGenerator) {
        this.cacheType = cacheType;
        this.keyGenerator = keyGenerator;
        this.cacheConfigs = new AtomicReference<>();
        this.statLogger = new CacheStatLogger();
    }

    /**
     * 初始化缓存
     */
    protected abstract void doInitialize(Map<String, CacheConfig> cacheConfigs);

    /**
     * 从缓存获取数据
     */
    protected abstract CacheItem<?> doGet(CacheConfig config, String key);

    /**
     * 批量从缓存获取数据
     */
    protected abstract Map<String, CacheItem<?>> doGetAll(CacheConfig config, Set<String> keys);

    /**
     * 把数据写入缓存
     */
    protected abstract void doPut(CacheConfig config, String key, CacheItem<?> item);

    /**
     * 批量把数据写入缓存
     */
    protected abstract void doPutAll(CacheConfig config, Map<String, CacheItem<?>> items);

    /**
     * 删除缓存数据
     */
    protected abstract void doRemove(CacheConfig config, String key);

    /**
     * 批量删除缓存数据
     */
    protected abstract void doRemoveAll(CacheConfig config, Set<String> keys);

    @Override
    public CacheType getType() {
        return cacheType;
    }

    @Override
    public void initialize(Map<String, CacheConfig> cacheConfigs) {
        if (this.cacheConfigs.compareAndSet(null, cacheConfigs)) {
            doInitialize(cacheConfigs);
        }
    }

    @Override
    public <K, V> CacheItem<V> get(String cacheName, K key) {
        CacheConfig config = cacheConfigs.get().get(cacheName);
        if (config == null) {
            throw new IllegalArgumentException("Unknown cache name: " + cacheName);
        }

        long begin = System.nanoTime();
        try {
            CacheItem<?> item = doGet(config, keyGenerator.generateFullKey(config, key));
            statLogger.addHitStat(cacheType, cacheName, 1, item != null ? 1 : 0);
            return (CacheItem<V>) item;
        } finally {
            statLogger.addQpmStat(cacheType, CacheOperationType.GET, cacheName, System.nanoTime() - begin);
        }
    }

    @Override
    public <K, V> Map<K, CacheItem<V>> getAll(String cacheName, Set<K> keys) {
        CacheConfig config = cacheConfigs.get().get(cacheName);
        if (config == null) {
            throw new IllegalArgumentException("Unknown cache name: " + cacheName);
        }

        long begin = System.nanoTime();
        try {
            Map<String, CacheItem<?>> itemsMap = doGetAll(config, keyGenerator.generateFullKeys(config, keys));

            Map<K, CacheItem<V>> results = new HashMap<>();
            for (K key : keys) {
                CacheItem<V> item = (CacheItem<V>) itemsMap.get(keyGenerator.generateFullKey(config, key));
                if (item != null) {
                    results.put(key, item);
                }
            }

            statLogger.addHitStat(cacheType, cacheName, keys.size(), results.size());
            return results;
        } finally {
            statLogger.addQpmStat(cacheType, CacheOperationType.GET_ALL, cacheName, System.nanoTime() - begin);
        }
    }

    @Override
    public <K, V> void put(String cacheName, K key, CacheItem<V> item) {
        CacheConfig config = cacheConfigs.get().get(cacheName);
        if (config == null) {
            throw new IllegalArgumentException("Unknown cache name: " + cacheName);
        }

        long begin = System.nanoTime();
        try {
            doPut(config, keyGenerator.generateFullKey(config, key), item);
        } finally {
            statLogger.addQpmStat(cacheType, CacheOperationType.PUT, cacheName, System.nanoTime() - begin);
        }
    }

    @Override
    public <K, V> void putAll(String cacheName, Map<K, CacheItem<V>> items) {
        CacheConfig config = cacheConfigs.get().get(cacheName);
        if (config == null) {
            throw new IllegalArgumentException("Unknown cache name: " + cacheName);
        }

        long begin = System.nanoTime();
        try {
            Map<String, CacheItem<?>> m = new HashMap<>();
            for (Entry<K, CacheItem<V>> entry : items.entrySet()) {
                m.put(keyGenerator.generateFullKey(config, entry.getKey()), entry.getValue());
            }

            doPutAll(config, m);
        } finally {
            statLogger.addQpmStat(cacheType, CacheOperationType.PUT_ALL, cacheName, System.nanoTime() - begin);
        }
    }

    @Override
    public <K, V> void remove(String cacheName, K key) {
        CacheConfig config = cacheConfigs.get().get(cacheName);
        if (config == null) {
            throw new IllegalArgumentException("Unknown cache name: " + cacheName);
        }

        long begin = System.nanoTime();
        try {
            doRemove(config, keyGenerator.generateFullKey(config, key));
        } finally {
            statLogger.addQpmStat(cacheType, CacheOperationType.REMOVE, cacheName, System.nanoTime() - begin);
        }
    }

    @Override
    public <K, V> void removeAll(String cacheName, Set<K> keys) {
        CacheConfig config = cacheConfigs.get().get(cacheName);
        if (config == null) {
            throw new IllegalArgumentException("Unknown cache name: " + cacheName);
        }

        long begin = System.nanoTime();
        try {
            doRemoveAll(config, keyGenerator.generateFullKeys(config, keys));
        } finally {
            statLogger.addQpmStat(cacheType, CacheOperationType.REMOVE_ALL, cacheName, System.nanoTime() - begin);
        }
    }
}
