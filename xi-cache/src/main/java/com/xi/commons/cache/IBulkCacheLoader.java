package com.xi.commons.cache;

import java.util.Map;
import java.util.Set;

/**
 * 批量缓存加载器
 */
@FunctionalInterface
public interface IBulkCacheLoader<K, V> {

    /**
     * 批量加载未命中缓存的数据
     */
    Map<K, V> load(Set<K> keys);

    /**
     * 判断加载的数据是否需要保存到缓存，默认为 true
     */
    default boolean shouldCache(V value) {
        return true;
    }
}
