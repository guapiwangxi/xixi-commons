package com.xi.commons.cache.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.xi.commons.cache.CacheConfig;
import com.xi.commons.cache.CacheItem;
import com.xi.commons.cache.CacheType;
import com.xi.commons.cache.IBulkCacheLoader;
import com.xi.commons.cache.ICacheClient;
import com.xi.commons.cache.ICacheLoader;
import com.xi.commons.cache.ICacheManager;
import com.xi.commons.cache.spring.SpringMultiLevelCacheManager;
import kotlin.collections.CollectionsKt;
import kotlin.collections.MapsKt;
import org.springframework.cache.CacheManager;

/**
 * 多级缓存管理器
 */
public class MultiLevelCacheManager implements ICacheManager {
    private static final int MAX_BATCH_KEYS = 128;
    private final AtomicBoolean initialized = new AtomicBoolean();
    private final CacheLoaderManager loaderManager = new CacheLoaderManager(this);
    private final Map<CacheType, ICacheClient> cacheClientsMap = new HashMap<>();
    private final Map<String, CacheConfig> cacheConfigsMap = new HashMap<>();

    @Override
    public void initialize() {
        if (initialized.compareAndSet(false, true)) {
            for (ICacheClient client : cacheClientsMap.values()) {
                client.initialize(cacheConfigsMap);
            }
        }
    }

    @Override
    public void addClient(ICacheClient client) {
        if (initialized.get()) {
            throw new IllegalStateException("Cannot configure the cache manager because it's already initialized.");
        } else {
            cacheClientsMap.put(client.getType(), client);
        }
    }

    @Override
    public Map<CacheType, ICacheClient> getClients() {
        return Collections.unmodifiableMap(cacheClientsMap);
    }

    @Override
    public void addConfig(CacheConfig config) {
        if (initialized.get()) {
            throw new IllegalStateException("Cannot configure the cache manager because it's already initialized.");
        } else {
            cacheConfigsMap.put(config.getName(), config);
        }
    }

    @Override
    public Map<String, CacheConfig> getConfigs() {
        return Collections.unmodifiableMap(cacheConfigsMap);
    }

    @Override
    public <K, V> CacheItem<V> lookup(String cacheName, K key) {
        if (!initialized.get()) {
            throw new IllegalStateException("Cannot use the cache manager because it's not initialized yet.");
        }

        CacheConfig config = cacheConfigsMap.get(cacheName);
        if (config == null) {
            throw new IllegalArgumentException("Unknown cache name: " + cacheName);
        }

        // 先从 L1 获取
        CacheItem<V> item = cacheClientsMap.get(config.getLevel1()).get(cacheName, key);

        // L1 找不到，再找 L2
        if (item == null && config.getLevel2() != null) {
            item = cacheClientsMap.get(config.getLevel2()).get(cacheName, key);

            // L2 找到了，回写 L1
            if (item != null) {
                cacheClientsMap.get(config.getLevel1()).put(cacheName, key, item);
            }
        }

        return item;
    }

    @Override
    public <K, V> V get(String cacheName, K key) {
        return get(cacheName, key, null);
    }

    @Override
    public <K, V> V get(String cacheName, K key, ICacheLoader<K, V> loader) {
        CacheItem<V> item = lookup(cacheName, key);

        // 对于找到的数据，如果 reload 时间已到，触发异步 reload
        if (item != null) {
            loaderManager.reloadValueAsync(cacheConfigsMap.get(cacheName), key, item, loader);
        }

        // 对于没找到的数据，调用 loader 加载
        if (item == null) {
            return loaderManager.loadValue(cacheConfigsMap.get(cacheName), key, loader);
        } else {
            return item.getValue();
        }
    }

    @Override
    public <K, V> Map<K, V> getAll(String cacheName, Set<K> keys) {
        return getAll(cacheName, keys, null);
    }

    @Override
    public <K, V> Map<K, V> getAll(String cacheName, Set<K> keys, IBulkCacheLoader<K, V> loader) {
        if (!initialized.get()) {
            throw new IllegalStateException("Cannot use the cache manager because it's not initialized yet.");
        }

        CacheConfig config = cacheConfigsMap.get(cacheName);
        if (config == null) {
            throw new IllegalArgumentException("Unknown cache name: " + cacheName);
        }

        if (keys.isEmpty()) {
            return Collections.emptyMap();
        }

        if (keys.size() <= MAX_BATCH_KEYS) {
            return doGetAll(config, keys, loader);
        } else {
            Map<K, V> results = new HashMap<>();
            for (List<K> chunk : CollectionsKt.chunked(keys, MAX_BATCH_KEYS)) {
                results.putAll(doGetAll(config, chunk, loader));
            }

            return results;
        }
    }

    private <K, V> Map<K, V> doGetAll(CacheConfig config, Collection<K> keys, IBulkCacheLoader<K, V> loader) {
        Set<K> remainKeys = new HashSet<>(keys);
        Map<K, CacheItem<V>> items = new HashMap<>(keys.size());

        // 先从 L1 获取
        Map<K, CacheItem<V>> l1Items = cacheClientsMap.get(config.getLevel1()).getAll(config.getName(), remainKeys);
        items.putAll(l1Items);
        remainKeys.removeAll(l1Items.keySet());

        // L1 没找到的数据，在 L2 里面找
        if (!remainKeys.isEmpty() && config.getLevel2() != null) {
            Map<K, CacheItem<V>> l2Items = cacheClientsMap.get(config.getLevel2()).getAll(config.getName(), remainKeys);
            items.putAll(l2Items);
            remainKeys.removeAll(l2Items.keySet());

            // 把 L2 找到的数据回写到 L1
            if (!l2Items.isEmpty()) {
                cacheClientsMap.get(config.getLevel1()).putAll(config.getName(), l2Items);
            }
        }

        // 对于找到的数据，如果 reload 时间已到，触发异步 reload
        if (!items.isEmpty()) {
            loaderManager.bulkReloadValuesAsync(config, items, loader);
        }

        // 对于仍然没找到的数据，调用 loader 加载
        Map<K, V> results = MapsKt.mapValues(items, e -> e.getValue().getValue());
        if (!remainKeys.isEmpty()) {
            results.putAll(loaderManager.bulkLoadValues(config, remainKeys, loader));
        }

        return results;
    }

    @Override
    public <K, V> void put(String cacheName, K key, V value) {
        if (!initialized.get()) {
            throw new IllegalStateException("Cannot use the cache manager because it's not initialized yet.");
        }

        CacheConfig config = cacheConfigsMap.get(cacheName);
        if (config == null) {
            throw new IllegalArgumentException("Unknown cache name: " + cacheName);
        }

        CacheItem<V> item;
        if (config.getReloadTime() != null) {
            item = new CacheItem<>(value, System.currentTimeMillis() + config.getReloadTime().toMillis());
        } else {
            item = new CacheItem<>(value);
        }

        // 保存到 L1
        cacheClientsMap.get(config.getLevel1()).put(cacheName, key, item);

        // 保存到 L2
        if (config.getLevel2() != null) {
            cacheClientsMap.get(config.getLevel2()).put(cacheName, key, item);
        }
    }

    @Override
    public <K, V> void putAll(String cacheName, Map<K, V> values) {
        if (!initialized.get()) {
            throw new IllegalStateException("Cannot use the cache manager because it's not initialized yet.");
        }

        CacheConfig config = cacheConfigsMap.get(cacheName);
        if (config == null) {
            throw new IllegalArgumentException("Unknown cache name: " + cacheName);
        }

        if (values.isEmpty()) {
            return;
        }

        if (values.size() <= MAX_BATCH_KEYS) {
            doPutAll(config, values.entrySet());
        } else {
            for (List<Entry<K, V>> chunk : CollectionsKt.chunked(values.entrySet(), MAX_BATCH_KEYS)) {
                doPutAll(config, chunk);
            }
        }
    }

    private <K, V> void doPutAll(CacheConfig config, Collection<Entry<K, V>> values) {
        long reloadTime = 0;
        if (config.getReloadTime() != null) {
            reloadTime = System.currentTimeMillis() + config.getReloadTime().toMillis();
        }

        Map<K, CacheItem<V>> items = new HashMap<>();
        for (Entry<K, V> entry : values) {
            items.put(entry.getKey(), new CacheItem<>(entry.getValue(), reloadTime));
        }

        // 保存到 L1
        cacheClientsMap.get(config.getLevel1()).putAll(config.getName(), items);

        // 保存到 L2
        if (config.getLevel2() != null) {
            cacheClientsMap.get(config.getLevel2()).putAll(config.getName(), items);
        }
    }

    @Override
    public <K, V> void remove(String cacheName, K key) {
        if (!initialized.get()) {
            throw new IllegalStateException("Cannot use the cache manager because it's not initialized yet.");
        }

        CacheConfig config = cacheConfigsMap.get(cacheName);
        if (config == null) {
            throw new IllegalArgumentException("Unknown cache name: " + cacheName);
        }

        // 从 L1 删除
        cacheClientsMap.get(config.getLevel1()).remove(cacheName, key);

        // 从 L2 删除
        if (config.getLevel2() != null) {
            cacheClientsMap.get(config.getLevel2()).remove(cacheName, key);
        }
    }

    @Override
    public <K, V> void removeAll(String cacheName, Set<K> keys) {
        if (!initialized.get()) {
            throw new IllegalStateException("Cannot use the cache manager because it's not initialized yet.");
        }

        CacheConfig config = cacheConfigsMap.get(cacheName);
        if (config == null) {
            throw new IllegalArgumentException("Unknown cache name: " + cacheName);
        }

        if (keys.isEmpty()) {
            return;
        }

        if (keys.size() <= MAX_BATCH_KEYS) {
            doRemoveAll(config, keys);
        } else {
            for (List<K> chunk : CollectionsKt.chunked(keys, MAX_BATCH_KEYS)) {
                doRemoveAll(config, new HashSet<>(chunk));
            }
        }
    }

    private <K, V> void doRemoveAll(CacheConfig config, Set<K> keys) {
        // 从 L1 删除
        cacheClientsMap.get(config.getLevel1()).removeAll(config.getName(), keys);

        // 从 L2 删除
        if (config.getLevel2() != null) {
            cacheClientsMap.get(config.getLevel2()).removeAll(config.getName(), keys);
        }
    }

    @Override
    public CacheManager asSpringCacheManager() {
        return new SpringMultiLevelCacheManager(this);
    }
}
