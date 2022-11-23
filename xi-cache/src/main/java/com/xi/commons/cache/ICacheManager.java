package com.xi.commons.cache;

import java.util.Map;
import java.util.Set;

import org.springframework.cache.CacheManager;

/**
 * 缓存管理器，文档参见 https://yuque.antfin-inc.com/social-growth/project/pm746l
 */
public interface ICacheManager {

    /**
     * 初始化缓存
     */
    void initialize();

    /**
     * 添加缓存客户端
     */
    void addClient(ICacheClient client);

    /**
     * 返回配置的缓存客户端
     */
    Map<CacheType, ICacheClient> getClients();

    /**
     * 添加缓存配置
     */
    void addConfig(CacheConfig config);

    /**
     * 返回所有缓存配置
     */
    Map<String, CacheConfig> getConfigs();

    /**
     * 从缓存获取数据，如果未命中缓存，返回 null，如果命中，返回 CacheItem（里面可能包含 null 值）
     */
    <K, V> CacheItem<V> lookup(String cacheName, K key);

    /**
     * 从缓存获取数据，此方法无法区分未命中和缓存的值本身是 null 的情况，如果需要区分，使用 lookup
     */
    <K, V> V get(String cacheName, K key);

    /**
     * 从缓存获取数据，如果未命中，调用 loader 加载，并发访问同样的 key 时，我们会保证单机内只有一个线程调用 loader，以避免缓存击穿
     */
    <K, V> V get(String cacheName, K key, ICacheLoader<K, V> loader);

    /**
     * 批量从缓存获取数据，如果未命中，返回的 Map 中不包含未命中的 key，如果命中，Map 中会包含命中的 key-value（value 本身可以是空）
     */
    <K, V> Map<K, V> getAll(String cacheName, Set<K> keys);

    /**
     * 批量从缓存获取数据，针对未命中的 key，调用 loader 加载，并发访问同样的 key 时，我们会保证单机内只有一个线程调用 loader，以避免缓存击穿
     */
    <K, V> Map<K, V> getAll(String cacheName, Set<K> keys, IBulkCacheLoader<K, V> loader);

    /**
     * 把数据写入缓存
     */
    <K, V> void put(String cacheName, K key, V value);

    /**
     * 批量把数据写入缓存
     */
    <K, V> void putAll(String cacheName, Map<K, V> values);

    /**
     * 删除缓存数据
     */
    <K, V> void remove(String cacheName, K key);

    /**
     * 批量删除缓存数据
     */
    <K, V> void removeAll(String cacheName, Set<K> keys);

    /**
     * 返回 Spring 的 CacheManager，适配 Spring 缓存框架，支持 @Cacheable, @CacheEvict 等注解
     */
    CacheManager asSpringCacheManager();
}
