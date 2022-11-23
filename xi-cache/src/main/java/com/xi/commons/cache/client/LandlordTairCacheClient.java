package com.xi.commons.cache.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import com.alibaba.global.landlord.Landlord;
import com.alibaba.global.landlord.model.TenantLocal;
import com.alibaba.global.satellite.proxy.tair.SatelliteTairManagerWrapper;
import com.alibaba.global.satellite.proxy.tair.impl.SatelliteCacheServiceImpl;
import com.alibaba.global.satellite.proxy.tair.wrapper.TairManagerWrapper;

import com.xi.commons.cache.CacheConfig;
import com.xi.commons.cache.CacheItem;
import com.xi.commons.cache.CacheType;
import com.xi.commons.cache.ICacheClient;
import com.taobao.tair.TairManager;

/**
 * 多租户 Tair 缓存客户端
 */
public class LandlordTairCacheClient implements ICacheClient {
    private final TenantLocal<TairCacheClient> tairCacheClient;

    public LandlordTairCacheClient(TairManagerWrapper tairManagerWrapper) {
        this(tairManagerWrapper, true, true);
    }

    public LandlordTairCacheClient(TairManagerWrapper tairManagerWrapper, boolean enableEnvPrefix, boolean enableTenantPrefix) {
        this.tairCacheClient = new TenantLocal<>(() -> {
            TairManager tairManager = getTairManager(tairManagerWrapper);
            int namespace = getNamespace(tairManagerWrapper);
            return new TairCacheClient(tairManager, namespace, enableEnvPrefix, enableTenantPrefix);
        });
    }

    private static TairManager getTairManager(TairManagerWrapper tairManagerWrapper) {
        // satellite 框架把所有租户的 TairManager 和 namespace 封装到一起，向下转型后即可重新取出当前租户的 TairManager
        TairManager tairManager = ((SatelliteCacheServiceImpl<?, ?>) tairManagerWrapper).getTairManager();

        // satellite 框架可能还会把默认的 TairManager 再包装一层，用来给缓存 key 添加压测标、环境、国家前缀
        // 由于我们自己也会做这些处理，所以需要取出原生的对象避免重复
        if (tairManager instanceof SatelliteTairManagerWrapper) {
            try {
                Method method = SatelliteTairManagerWrapper.class.getDeclaredMethod("getTairManager");
                method.setAccessible(true);
                return (TairManager) method.invoke(tairManager);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
                return tairManager;
            }
        }

        return tairManager;
    }

    private static int getNamespace(TairManagerWrapper tairManagerWrapper) {
        // satellite 框架把所有租户的 TairManager 和 namespace 封装到一起，向下转型后即可重新取出当前租户的 namespace
        return ((SatelliteCacheServiceImpl<?, ?>) tairManagerWrapper).getNamespace();
    }

    @Override
    public CacheType getType() {
        return CacheType.TAIR;
    }

    @Override
    public void initialize(Map<String, CacheConfig> cacheConfigs) {
        Landlord.runAllTenant(() -> tairCacheClient.get().initialize(cacheConfigs));
    }

    @Override
    public <K, V> CacheItem<V> get(String cacheName, K key) {
        return tairCacheClient.get().get(cacheName, key);
    }

    @Override
    public <K, V> Map<K, CacheItem<V>> getAll(String cacheName, Set<K> keys) {
        return tairCacheClient.get().getAll(cacheName, keys);
    }

    @Override
    public <K, V> void put(String cacheName, K key, CacheItem<V> item) {
        tairCacheClient.get().put(cacheName, key, item);
    }

    @Override
    public <K, V> void putAll(String cacheName, Map<K, CacheItem<V>> items) {
        tairCacheClient.get().putAll(cacheName, items);
    }

    @Override
    public <K, V> void remove(String cacheName, K key) {
        tairCacheClient.get().remove(cacheName, key);
    }

    @Override
    public <K, V> void removeAll(String cacheName, Set<K> keys) {
        tairCacheClient.get().removeAll(cacheName, keys);
    }
}
