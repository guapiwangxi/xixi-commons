package com.xi.commons.cache;

/**
 * 缓存加载器
 */
@FunctionalInterface
public interface ICacheLoader<K, V> {

    /**
     * 加载未命中缓存的数据
     */
    V load(K key);

    /**
     * 判断加载的数据是否需要保存到缓存，默认为 true
     */
    default boolean shouldCache(V value) {
        return true;
    }
}
