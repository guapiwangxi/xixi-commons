package com.xi.commons.cache.spring;

import java.util.concurrent.Callable;

import com.xi.commons.cache.CacheItem;
import com.xi.commons.cache.ICacheManager;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

/**
 * 适配 Spring 缓存
 */
public class SpringMultiLevelCache implements Cache {
    private final ICacheManager cacheManager;
    private final String name;

    SpringMultiLevelCache(ICacheManager cacheManager, String name) {
        this.cacheManager = cacheManager;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return cacheManager;
    }

    @Override
    public ValueWrapper get(Object key) {
        CacheItem<?> item = cacheManager.lookup(name, key);
        if (item == null) {
            return null;
        } else {
            return new SimpleValueWrapper(item.getValue());
        }
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        T value = cacheManager.get(name, key);
        if (value != null && type != null && !type.isInstance(value)) {
            throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value);
        }

        return value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return cacheManager.get(name, key, k -> {
            try {
                return valueLoader.call();
            } catch (Throwable e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }
        });
    }

    @Override
    public void put(Object key, Object value) {
        cacheManager.put(name, key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        // 除非使用分布式锁，否则多级缓存无法原子性实现 putIfAbsent 操作
        throw new UnsupportedOperationException("putIfAbsent is not supported for multi level caches.");
    }

    @Override
    public void evict(Object key) {
        cacheManager.remove(name, key);
    }

    @Override
    public void clear() {
        // Tair 不支持批量删除指定前缀的数据，Redis 可使用 scan 实现但性能低下，因此不支持 clear 操作
        throw new UnsupportedOperationException("clear is not supported for multi level caches.");
    }
}
