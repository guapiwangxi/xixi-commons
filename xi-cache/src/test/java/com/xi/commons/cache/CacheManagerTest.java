package com.xi.commons.cache;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import com.xi.commons.cache.client.GuavaCacheClient;
import com.xi.commons.cache.client.RedisCacheClient;
import com.xi.commons.cache.client.TairCacheClient;
import com.xi.commons.cache.impl.MultiLevelCacheManager;
import com.taobao.eagleeye.redis.clients.jedis.JedisPool;
import com.taobao.eagleeye.redis.clients.jedis.JedisPoolConfig;
import com.taobao.tair.impl.mc.MultiClusterTairManager;
import kotlin.collections.CollectionsKt;
import kotlin.collections.SetsKt;
import org.junit.Assert;
import org.junit.Test;

import static java.util.stream.Collectors.toSet;

/**
 * Created by vince at Jun 17, 2022.
 */
public class CacheManagerTest {

    @Test
    public void testGuava() {
        ICacheManager cacheManager = new MultiLevelCacheManager();
        cacheManager.addClient(new GuavaCacheClient());
        cacheManager.addConfig(
            CacheConfig.builder()
                .name("name")
                .expireTime(Duration.ofMinutes(5))
                .level1(CacheType.GUAVA)
                .build());
        cacheManager.initialize();

        testCrud(cacheManager);
        testConcurrentBulkLoading(cacheManager);
        testConcurrentLoading(cacheManager);
    }

    @Test
    public void testTair() {
        MultiClusterTairManager tairManager = new MultiClusterTairManager();
        tairManager.setUserName("045ab2c6533a43f4");
        tairManager.init();

        ICacheManager cacheManager = new MultiLevelCacheManager();
        cacheManager.addClient(new TairCacheClient(tairManager, 1344));
        cacheManager.addConfig(
            CacheConfig.builder()
                .name("name")
                .expireTime(Duration.ofMinutes(5))
                .level1(CacheType.TAIR)
                .build());
        cacheManager.initialize();

        testCrud(cacheManager);
        testConcurrentBulkLoading(cacheManager);
        testConcurrentLoading(cacheManager);
    }

    @Test
    public void testRedis() {
        JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), URI.create("redis://127.0.0.1:6379/0"));

        ICacheManager cacheManager = new MultiLevelCacheManager();
        cacheManager.addClient(new RedisCacheClient(jedisPool));
        cacheManager.addConfig(
            CacheConfig.builder()
                .name("name")
                .expireTime(Duration.ofMinutes(5))
                .level1(CacheType.REDIS)
                .build());
        cacheManager.initialize();

        testCrud(cacheManager);
        testConcurrentBulkLoading(cacheManager);
        testConcurrentLoading(cacheManager);
    }

    @Test
    public void testGuavaAndRedis() {
        JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), URI.create("redis://127.0.0.1:6379/0"));

        ICacheManager cacheManager = new MultiLevelCacheManager();
        cacheManager.addClient(new GuavaCacheClient());
        cacheManager.addClient(new RedisCacheClient(jedisPool));
        cacheManager.addConfig(
            CacheConfig.builder()
                .name("name")
                .expireTime(Duration.ofMinutes(5))
                .level1(CacheType.GUAVA)
                .level2(CacheType.REDIS)
                .build());
        cacheManager.initialize();

        testCrud(cacheManager);
        testConcurrentBulkLoading(cacheManager);
        testConcurrentLoading(cacheManager);

        // 暂停 2 分钟观察 stat 日志
        try {
            TimeUnit.MINUTES.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void testCrud(ICacheManager cacheManager) {
        // 触发缓存加载
        String value1 = cacheManager.get("name", "key1", k -> "value1");
        Assert.assertEquals("value1", value1);

        Set<String> keys = SetsKt.setOf("key2", "key3");
        Map<String, String> values = cacheManager.getAll("name", keys, k -> {
            Map<String, String> m = new HashMap<>();
            m.put("key2", "value2");
            m.put("key3", "value3");
            return m;
        });

        Assert.assertEquals(2, values.size());
        Assert.assertEquals("value2", values.get("key2"));
        Assert.assertEquals("value3", values.get("key3"));

        // 查询（命中缓存）
        value1 = cacheManager.get("name", "key1");
        Assert.assertEquals("value1", value1);

        values = cacheManager.getAll("name", keys);
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("value2", values.get("key2"));
        Assert.assertEquals("value3", values.get("key3"));

        // 删除后查询（未命中）
        cacheManager.remove("name", "key1");
        value1 = cacheManager.get("name", "key1");
        Assert.assertNull(value1);

        cacheManager.removeAll("name", keys);
        values = cacheManager.getAll("name", keys);
        Assert.assertTrue(values.isEmpty());
    }

    private void testConcurrentLoading(ICacheManager cacheManager) {
        CountDownLatch latch = new CountDownLatch(10);

        ICacheLoader<Integer, String> loader = key -> {
            System.out.printf("[%s] loading key: %s\n", Thread.currentThread().getName(), key);

            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return "value" + key;
        };

        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(() -> {
                System.out.printf("[%s] start get.\n", Thread.currentThread().getName());
                long start = System.currentTimeMillis();

                String value = cacheManager.get("name", 110, loader);

                long elapsed = System.currentTimeMillis() - start;
                System.out.printf("[%s] finish get, time elapsed: %d, value: %s\n", Thread.currentThread().getName(), elapsed, value);
                latch.countDown();
            });

            thread.setName("Single-" + i);
            thread.start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void testConcurrentBulkLoading(ICacheManager cacheManager) {
        CountDownLatch latch = new CountDownLatch(50);

        IBulkCacheLoader<Integer, String> loader = keys -> {
            System.out.printf("[%s] loading keys: %s\n", Thread.currentThread().getName(), new TreeSet<>(keys));

            try {
                TimeUnit.MILLISECONDS.sleep(keys.size() * 100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return CollectionsKt.associateWith(keys, k -> "value" + k);
        };

        for (int i = 0; i < 50; i++) {
            Thread thread = new Thread(() -> {
                System.out.printf("[%s] start getAll.\n", Thread.currentThread().getName());
                long start = System.currentTimeMillis();

                Set<Integer> keys = IntStream.rangeClosed(1, 10).boxed().collect(toSet());
                Map<Integer, String> values = cacheManager.getAll("name", keys, loader);

                long elapsed = System.currentTimeMillis() - start;
                System.out.printf("[%s] finish getAll, time elapsed: %d, values: %s\n", Thread.currentThread().getName(), elapsed, new TreeMap<>(values));
                latch.countDown();
            });

            thread.setName("Bulk-" + i);
            thread.start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
