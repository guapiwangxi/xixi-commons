package com.xi.commons.cache;

import java.util.Map;
import java.util.Set;

/**
 * 缓存客户端
 */
public interface ICacheClient {

    /**
     * 缓存类型
     */
    CacheType getType();

    /**
     * 初始化缓存
     */
    void initialize(Map<String, CacheConfig> cacheConfigs);

    /**
     * 从缓存获取数据
     */
    <K, V> CacheItem<V> get(String cacheName, K key);

    /**
     * 批量从缓存获取数据
     */
    <K, V> Map<K, CacheItem<V>> getAll(String cacheName, Set<K> keys);

    /**
     * 把数据写入缓存
     */
    <K, V> void put(String cacheName, K key, CacheItem<V> item);

    /**
     * 批量把数据写入缓存
     */
    <K, V> void putAll(String cacheName, Map<K, CacheItem<V>> items);

    /**
     * 删除缓存数据
     */
    <K, V> void remove(String cacheName, K key);

    /**
     * 批量删除缓存数据
     */
    <K, V> void removeAll(String cacheName, Set<K> keys);
}
