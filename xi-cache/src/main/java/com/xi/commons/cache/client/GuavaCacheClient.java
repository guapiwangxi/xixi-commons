package com.xi.commons.cache.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xi.commons.cache.CacheConfig;
import com.xi.commons.cache.CacheItem;
import com.xi.commons.cache.CacheType;
import com.xi.commons.cache.impl.CacheKeyGenerator;

/**
 * Guava 缓存客户端
 */
public class GuavaCacheClient extends AbstractCacheClient {
    private final Map<String, Cache<String, CacheItem<?>>> cachesMap = new HashMap<>();

    public GuavaCacheClient() {
        this(true);
    }

    public GuavaCacheClient(boolean enableTenantPrefix) {
        super(CacheType.GUAVA, new CacheKeyGenerator.DefaultCacheKeyGenerator(true, false, enableTenantPrefix));
    }

    @Override
    protected void doInitialize(Map<String, CacheConfig> cacheConfigs) {
        for (CacheConfig config : cacheConfigs.values()) {
            // 如果 L1 配置为 Guava，初始化相应的缓存对象；忽略 L2 配置，一般不会把 Guava 配置为 L2 缓存
            if (config.getLevel1() == CacheType.GUAVA) {
                Cache<String, CacheItem<?>> cache = CacheBuilder.newBuilder()
                    .expireAfterWrite(config.getExpireTime().toMillis(), TimeUnit.MILLISECONDS)
                    .maximumSize(10000)
                    .concurrencyLevel(1)
                    .build();

                cachesMap.put(config.getName(), cache);
            }
        }
    }

    @Override
    protected CacheItem<?> doGet(CacheConfig config, String key) {
        Cache<String, CacheItem<?>> cache = cachesMap.get(config.getName());
        if (cache == null) {
            throw new IllegalArgumentException("Unknown cache name: " + config.getName());
        } else {
            return cache.getIfPresent(key);
        }
    }

    @Override
    protected Map<String, CacheItem<?>> doGetAll(CacheConfig config, Set<String> keys) {
        Cache<String, CacheItem<?>> cache = cachesMap.get(config.getName());
        if (cache == null) {
            throw new IllegalArgumentException("Unknown cache name: " + config.getName());
        } else {
            return cache.getAllPresent(keys);
        }
    }

    @Override
    protected void doPut(CacheConfig config, String key, CacheItem<?> item) {
        Cache<String, CacheItem<?>> cache = cachesMap.get(config.getName());
        if (cache == null) {
            throw new IllegalArgumentException("Unknown cache name: " + config.getName());
        } else {
            cache.put(key, item);
        }
    }

    @Override
    protected void doPutAll(CacheConfig config, Map<String, CacheItem<?>> items) {
        Cache<String, CacheItem<?>> cache = cachesMap.get(config.getName());
        if (cache == null) {
            throw new IllegalArgumentException("Unknown cache name: " + config.getName());
        } else {
            cache.putAll(items);
        }
    }

    @Override
    protected void doRemove(CacheConfig config, String key) {
        Cache<String, CacheItem<?>> cache = cachesMap.get(config.getName());
        if (cache == null) {
            throw new IllegalArgumentException("Unknown cache name: " + config.getName());
        } else {
            cache.invalidate(key);
        }
    }

    @Override
    protected void doRemoveAll(CacheConfig config, Set<String> keys) {
        Cache<String, CacheItem<?>> cache = cachesMap.get(config.getName());
        if (cache == null) {
            throw new IllegalArgumentException("Unknown cache name: " + config.getName());
        } else {
            cache.invalidateAll(keys);
        }
    }
}
