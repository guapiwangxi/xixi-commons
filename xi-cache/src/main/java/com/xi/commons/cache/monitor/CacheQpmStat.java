package com.xi.commons.cache.monitor;

import java.util.concurrent.atomic.AtomicLong;

import com.xi.commons.cache.CacheType;

/**
 * 缓存 qpm/rt 统计
 */
public class CacheQpmStat {
    private final CacheType cacheType;
    private final CacheOperationType operationType;
    private final String cacheName;
    private final AtomicLong qpm;
    private final AtomicLong rt;

    CacheQpmStat(CacheType cacheType, CacheOperationType operationType, String cacheName) {
        this.cacheType = cacheType;
        this.operationType = operationType;
        this.cacheName = cacheName;
        this.qpm = new AtomicLong();
        this.rt = new AtomicLong();
    }

    public CacheType getCacheType() {
        return cacheType;
    }

    public CacheOperationType getOperationType() {
        return operationType;
    }

    public String getCacheName() {
        return cacheName;
    }

    public long getQpm() {
        return qpm.get();
    }

    public long getRt() {
        return rt.get();
    }

    public void addStat(long rt) {
        this.qpm.incrementAndGet();
        this.rt.addAndGet(rt);
    }
}
