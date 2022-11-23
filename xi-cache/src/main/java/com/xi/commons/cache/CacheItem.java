package com.xi.commons.cache;

import java.io.Serializable;

/**
 * 缓存项，这个类是真正保存到缓存里面的内容，我们的值存放在 value 字段中
 */
public class CacheItem<V> implements Serializable {
    private static final long serialVersionUID = 1L;
    private final V value;
    private final long reloadTime;

    public CacheItem(V value) {
        this(value, 0L);
    }

    public CacheItem(V value, long reloadTime) {
        this.value = value;
        this.reloadTime = reloadTime;
    }

    public V getValue() {
        return value;
    }

    public long getReloadTime() {
        return reloadTime;
    }
}
