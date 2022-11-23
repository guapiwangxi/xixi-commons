package com.xi.commons.cache;

import java.time.Duration;

import org.apache.commons.lang3.StringUtils;

/**
 * 缓存配置
 */
public class CacheConfig {

    /**
     * 缓存名
     */
    private final String name;

    /**
     * 版本号
     */
    private final String version;

    /**
     * 缓存过期时间
     */
    private final Duration expireTime;

    /**
     * 缓存重加载时间，从写入时开始计时，时间达到后触发异步重加载
     */
    private final Duration reloadTime;

    /**
     * L1 缓存的类型
     */
    private final CacheType level1;

    /**
     * L2 缓存的类型
     */
    private final CacheType level2;

    private CacheConfig(Builder builder) {
        if (StringUtils.isEmpty(builder.name)) {
            throw new IllegalArgumentException("name is required. ");
        }
        if (builder.expireTime == null) {
            throw new IllegalArgumentException("expireTime is required. ");
        }
        if (builder.level1 == null) {
            throw new IllegalArgumentException("level1 is required. ");
        }

        this.name = builder.name;
        this.version = builder.version;
        this.expireTime = builder.expireTime;
        this.reloadTime = builder.reloadTime;
        this.level1 = builder.level1;
        this.level2 = builder.level2;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Duration getExpireTime() {
        return expireTime;
    }

    public Duration getReloadTime() {
        return reloadTime;
    }

    public CacheType getLevel1() {
        return level1;
    }

    public CacheType getLevel2() {
        return level2;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String version;
        private Duration expireTime;
        private Duration reloadTime;
        private CacheType level1;
        private CacheType level2;

        Builder() { }

        public String name() {
            return name;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public String version() {
            return version;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Duration expireTime() {
            return expireTime;
        }

        public Builder expireTime(Duration expireTime) {
            this.expireTime = expireTime;
            return this;
        }

        public Duration reloadTime() {
            return reloadTime;
        }

        public Builder reloadTime(Duration reloadTime) {
            this.reloadTime = reloadTime;
            return this;
        }

        public CacheType level1() {
            return level1;
        }

        public Builder level1(CacheType level1) {
            this.level1 = level1;
            return this;
        }

        public CacheType level2() {
            return level2;
        }

        public Builder level2(CacheType level2) {
            this.level2 = level2;
            return this;
        }

        public CacheConfig build() {
            return new CacheConfig(this);
        }
    }
}
