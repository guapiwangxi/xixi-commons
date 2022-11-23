package com.xi.commons.cache.impl;

import java.util.Set;

import com.alibaba.global.common.utils.EnvUtils;
import com.alibaba.global.landlord.LandlordContext;
import com.alibaba.global.landlord.exception.LandlordNotFoundException;

import com.xi.commons.cache.CacheConfig;
import com.taobao.common.fulllinkstresstesting.StressTestingUtil;
import org.apache.commons.lang3.StringUtils;

import static java.util.stream.Collectors.toSet;

/**
 * 缓存 key 生成器
 */
public abstract class CacheKeyGenerator {

    /**
     * 是否添加压测前缀 __test_
     */
    protected abstract boolean enableTestPrefix();

    /**
     * 是否添加环境前缀 daily#, staging#, live#
     */
    protected abstract boolean enableEnvPrefix();

    /**
     * 是否添加租户前缀 LAZADA_SG@
     */
    protected abstract boolean enableTenantPrefix();

    /**
     * 批量生成缓存 key
     */
    public Set<String> generateFullKeys(CacheConfig config, Set<?> keys) {
        return keys.stream().map(k -> generateFullKey(config, k)).collect(toSet());
    }

    /**
     * 生成缓存 key，格式为 __test_live#LAZADA_SG@cache_name_v1_key
     */
    public String generateFullKey(CacheConfig config, Object key) {
        StringBuilder sb = new StringBuilder();

        // 压测流量添加 __test_ 前缀
        if (enableTestPrefix() && StressTestingUtil.isTestFlow()) {
            sb.append("__test_");
        }

        // 添加环境前缀
        if (enableEnvPrefix()) {
            sb.append(EnvUtils.isDaily() ? "daily#" : EnvUtils.isPre() ? "staging#" : "live#");
        }

        // 添加租户 ID 前缀
        if (enableTenantPrefix()) {
            String tenantId = LandlordContext.getCurrentTenantId();
            if (tenantId == null) {
                throw new LandlordNotFoundException("Cannot get the current tenant ID.");
            }

            sb.append(tenantId).append('@');
        }

        // 最终格式为 __test_live#LAZADA_SG@cache_name_v1_key
        if (StringUtils.isNotEmpty(config.getVersion())) {
            return sb.append(config.getName()).append('_').append(config.getVersion()).append('_').append(key).toString();
        } else {
            return sb.append(config.getName()).append('_').append(key).toString();
        }
    }

    /**
     * 默认的缓存 key 生成器，在构造方法指定是否添加各类前缀
     */
    public static class DefaultCacheKeyGenerator extends CacheKeyGenerator {
        private final boolean enableTestPrefix;
        private final boolean enableEnvPrefix;
        private final boolean enableTenantPrefix;

        public DefaultCacheKeyGenerator(boolean enableTestPrefix, boolean enableEnvPrefix, boolean enableTenantPrefix) {
            this.enableTestPrefix = enableTestPrefix;
            this.enableEnvPrefix = enableEnvPrefix;
            this.enableTenantPrefix = enableTenantPrefix;
        }

        @Override
        protected boolean enableTestPrefix() {
            return enableTestPrefix;
        }

        @Override
        protected boolean enableEnvPrefix() {
            return enableEnvPrefix;
        }

        @Override
        protected boolean enableTenantPrefix() {
            return enableTenantPrefix;
        }
    }
}
