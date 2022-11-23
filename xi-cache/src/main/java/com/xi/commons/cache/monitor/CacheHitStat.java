package com.xi.commons.cache.monitor;

import java.util.concurrent.atomic.AtomicLong;

import com.xi.commons.cache.CacheType;

/**
 * 缓存命中率统计
 */
public class CacheHitStat {
    private final CacheType cacheType;
    private final String cacheName;
    private final AtomicLong totalCount;
    private final AtomicLong hitCount;

    CacheHitStat(CacheType cacheType, String cacheName) {
        this.cacheType = cacheType;
        this.cacheName = cacheName;
        this.totalCount = new AtomicLong();
        this.hitCount = new AtomicLong();
    }

    public CacheType getCacheType() {
        return cacheType;
    }

    public String getCacheName() {
        return cacheName;
    }

    public long getTotalCount() {
        return totalCount.get();
    }

    public long getHitCount() {
        return hitCount.get();
    }

    public void addStat(long totalCount, long hitCount) {
        this.totalCount.addAndGet(totalCount);
        this.hitCount.addAndGet(hitCount);
    }
}
