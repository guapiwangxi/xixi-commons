package com.xi.commons.cache.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.xi.commons.cache.CacheConfig;
import com.xi.commons.cache.CacheItem;
import com.xi.commons.cache.CacheType;
import com.xi.commons.cache.impl.CacheException;
import com.xi.commons.cache.impl.CacheKeyGenerator;
import com.taobao.tair.DataEntry;
import com.taobao.tair.PressiveTestController;
import com.taobao.tair.Result;
import com.taobao.tair.ResultCode;
import com.taobao.tair.TairManager;

/**
 * Tair 缓存客户端
 */
public class TairCacheClient extends AbstractCacheClient {
    private final TairManager tairManager;
    private final int namespace;

    public TairCacheClient(TairManager tairManager, int namespace) {
        this(tairManager, namespace, true, true);
    }

    public TairCacheClient(TairManager tairManager, int namespace, boolean enableEnvPrefix, boolean enableTenantPrefix) {
        super(CacheType.TAIR, new TairCacheKeyGenerator(enableEnvPrefix, enableTenantPrefix));
        this.tairManager = tairManager;
        this.namespace = namespace;
    }

    private static class TairCacheKeyGenerator extends CacheKeyGenerator {
        private final boolean enableEnvPrefix;
        private final boolean enableTenantPrefix;

        TairCacheKeyGenerator(boolean enableEnvPrefix, boolean enableTenantPrefix) {
            this.enableEnvPrefix = enableEnvPrefix;
            this.enableTenantPrefix = enableTenantPrefix;
        }

        @Override
        protected boolean enableTestPrefix() {
            return !PressiveTestController.getSwitchPressive(); // 如果 tair 内部也会添加压测前缀，我们不需要重复处理
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

    @Override
    protected void doInitialize(Map<String, CacheConfig> cacheConfigs) {
        // do nothing...
    }

    @Override
    protected CacheItem<?> doGet(CacheConfig config, String key) {
        Result<DataEntry> result = tairManager.get(namespace, key);
        if (!result.isSuccess()) {
            throw new CacheException("Tair get failed: " + result.getRc());
        }

        DataEntry data = result.getValue();
        if (data == null) {
            return null;
        } else {
            return (CacheItem<?>) data.getValue();
        }
    }

    @Override
    protected Map<String, CacheItem<?>> doGetAll(CacheConfig config, Set<String> keys) {
        Result<List<DataEntry>> result = tairManager.mget(namespace, new ArrayList<>(keys));
        if (!result.isSuccess()) {
            throw new CacheException("Tair mget failed: " + result.getRc());
        }

        Map<String, CacheItem<?>> items = new HashMap<>();
        for (DataEntry data : result.getValue()) {
            CacheItem<?> item = (CacheItem<?>) data.getValue();
            if (item != null) {
                items.put(data.getKey().toString(), item);
            }
        }

        return items;
    }

    @Override
    protected void doPut(CacheConfig config, String key, CacheItem<?> item) {
        ResultCode rc = tairManager.put(namespace, key, item, 0, (int) config.getExpireTime().getSeconds());
        if (!rc.isSuccess()) {
            throw new CacheException("Tair put failed: " + rc);
        }
    }

    @Override
    protected void doPutAll(CacheConfig config, Map<String, CacheItem<?>> items) {
        // tair mput 命令已不能使用，只能循环调用 put 实现
        for (Entry<String, CacheItem<?>> entry : items.entrySet()) {
            doPut(config, entry.getKey(), entry.getValue());
        }
    }

    @Override
    protected void doRemove(CacheConfig config, String key) {
        ResultCode rc = tairManager.delete(namespace, key);
        if (!rc.isSuccess()) {
            throw new CacheException("Tair delete failed: " + rc);
        }
    }

    @Override
    protected void doRemoveAll(CacheConfig config, Set<String> keys) {
        ResultCode rc = tairManager.mdelete(namespace, new ArrayList<>(keys));
        if (!rc.isSuccess()) {
            throw new CacheException("Tair mdelete failed: " + rc);
        }
    }
}
