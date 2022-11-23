package com.xi.commons.cache.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.xi.commons.cache.CacheConfig;
import com.xi.commons.cache.CacheItem;
import com.xi.commons.cache.CacheType;
import com.taobao.eagleeye.redis.clients.jedis.Jedis;
import com.taobao.eagleeye.redis.clients.jedis.JedisPool;
import com.xi.commons.cache.impl.CacheKeyGenerator;
import org.apache.commons.lang3.SerializationUtils;

/**
 * Redis 缓存客户端
 */
public class RedisCacheClient extends AbstractCacheClient {
    private final JedisPool jedisPool;

    public RedisCacheClient(JedisPool jedisPool) {
        this(jedisPool, true, true);
    }

    public RedisCacheClient(JedisPool jedisPool, boolean enableEnvPrefix, boolean enableTenantPrefix) {
        super(CacheType.REDIS, new CacheKeyGenerator.DefaultCacheKeyGenerator(true, enableEnvPrefix, enableTenantPrefix));
        this.jedisPool = jedisPool;
    }

    @Override
    protected void doInitialize(Map<String, CacheConfig> cacheConfigs) {
        // do nothing...
    }

    @Override
    protected CacheItem<?> doGet(CacheConfig config, String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            byte[] data = jedis.get(key.getBytes());
            if (data == null) {
                return null;
            } else {
                return SerializationUtils.deserialize(data);
            }
        }
    }

    @Override
    protected Map<String, CacheItem<?>> doGetAll(CacheConfig config, Set<String> keys) {
        try (Jedis jedis = jedisPool.getResource()) {
            List<byte[]> results = jedis.mget(keys.stream().map(String::getBytes).toArray(byte[][]::new));
            Map<String, CacheItem<?>> items = new HashMap<>();

            int i = 0;
            for (String key : keys) {
                byte[] data = results.get(i++);
                if (data != null) {
                    items.put(key, SerializationUtils.deserialize(data));
                }
            }

            return items;
        }
    }

    @Override
    protected void doPut(CacheConfig config, String key, CacheItem<?> item) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key.getBytes(), (int) config.getExpireTime().getSeconds(), SerializationUtils.serialize(item));
        }
    }

    @Override
    protected void doPutAll(CacheConfig config, Map<String, CacheItem<?>> items) {
        // mset 命令不支持设置过期时间，因此我们只能批量执行 setex 命令，然而 pipeline 在集群模式下也会受限
        // 所以为了最大的兼容性，我们只能循环执行 setex，性能稍微没那么好
        try (Jedis jedis = jedisPool.getResource()) {
            for (Entry<String, CacheItem<?>> entry : items.entrySet()) {
                byte[] key = entry.getKey().getBytes();
                byte[] value = SerializationUtils.serialize(entry.getValue());
                jedis.setex(key, (int) config.getExpireTime().getSeconds(), value);
            }
        }
    }

    @Override
    protected void doRemove(CacheConfig config, String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key.getBytes());
        }
    }

    @Override
    protected void doRemoveAll(CacheConfig config, Set<String> keys) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(keys.stream().map(String::getBytes).toArray(byte[][]::new));
        }
    }
}
