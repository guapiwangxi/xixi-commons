package com.xi.commons.cache.client;

import java.util.Map;
import java.util.Set;

import com.alibaba.global.landlord.Landlord;
import com.alibaba.global.landlord.model.TenantLocal;

import com.xi.commons.cache.CacheConfig;
import com.xi.commons.cache.CacheItem;
import com.xi.commons.cache.CacheType;
import com.xi.commons.cache.ICacheClient;
import com.taobao.eagleeye.redis.clients.jedis.JedisPool;

/**
 * Created by vince at Jun 20, 2022.
 */
public class LandlordRedisCacheClient implements ICacheClient {
    private final TenantLocal<RedisCacheClient> redisCacheClient;

    public LandlordRedisCacheClient(TenantLocal<JedisPool> jedisPool) {
        this(jedisPool, true, true);
    }

    public LandlordRedisCacheClient(TenantLocal<JedisPool> jedisPool, boolean enableEnvPrefix, boolean enableTenantPrefix) {
        this.redisCacheClient = new TenantLocal<>(() -> new RedisCacheClient(jedisPool.get(), enableEnvPrefix, enableTenantPrefix));
    }

    @Override
    public CacheType getType() {
        return CacheType.REDIS;
    }

    @Override
    public void initialize(Map<String, CacheConfig> cacheConfigs) {
        Landlord.runAllTenant(() -> redisCacheClient.get().initialize(cacheConfigs));
    }

    @Override
    public <K, V> CacheItem<V> get(String cacheName, K key) {
        return redisCacheClient.get().get(cacheName, key);
    }

    @Override
    public <K, V> Map<K, CacheItem<V>> getAll(String cacheName, Set<K> keys) {
        return redisCacheClient.get().getAll(cacheName, keys);
    }

    @Override
    public <K, V> void put(String cacheName, K key, CacheItem<V> item) {
        redisCacheClient.get().put(cacheName, key, item);
    }

    @Override
    public <K, V> void putAll(String cacheName, Map<K, CacheItem<V>> items) {
        redisCacheClient.get().putAll(cacheName, items);
    }

    @Override
    public <K, V> void remove(String cacheName, K key) {
        redisCacheClient.get().remove(cacheName, key);
    }

    @Override
    public <K, V> void removeAll(String cacheName, Set<K> keys) {
        redisCacheClient.get().removeAll(cacheName, keys);
    }
}
