package com.xi.commons.cache.spring;

import java.util.Collection;

import com.xi.commons.cache.ICacheManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * 适配 Spring 缓存管理器
 */
public class SpringMultiLevelCacheManager implements CacheManager, InitializingBean {
    private final ICacheManager cacheManager;

    public SpringMultiLevelCacheManager(ICacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void afterPropertiesSet() {
        cacheManager.initialize();
    }

    @Override
    public Cache getCache(String name) {
        if (cacheManager.getConfigs().containsKey(name)) {
            return new SpringMultiLevelCache(cacheManager, name);
        } else {
            return null;
        }
    }

    @Override
    public Collection<String> getCacheNames() {
        return cacheManager.getConfigs().keySet();
    }
}
