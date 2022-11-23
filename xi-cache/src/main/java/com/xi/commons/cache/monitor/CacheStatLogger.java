package com.xi.commons.cache.monitor;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.lazada.zal.commons.async.NamedThreadFactory;
import com.xi.commons.cache.CacheType;
import com.lazada.zal.commons.logger.BizLoggerImpl;

/**
 * 缓存统计日志
 */
public class CacheStatLogger {
    // 一级 key: 缓存的唯一标识，如 cacheType + cacheName
    // 二级 key: 每分钟的起始时间戳
    // value: 当前分钟的统计数据
    private final Map<String, Map<Long, CacheQpmStat>> qpmStats = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, CacheHitStat>> hitStats = new ConcurrentHashMap<>();

    /**
     * 初始化日志打印的调度任务，每 10s 执行一次
     */
    public CacheStatLogger() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("CacheStat-", true));
        executor.scheduleAtFixedRate(this::printStatLogs, 10, 10, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                executor.shutdown();

                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ignored) {
                // ignored...
            }
        }));
    }

    /**
     * 统计缓存操作的 qpm/rt 数据
     */
    public void addQpmStat(CacheType cacheType, CacheOperationType operationType, String cacheName, long rt) {
        // 初始化当前缓存的数据
        String key = cacheType + "_" + operationType + "_" + cacheName;
        Map<Long, CacheQpmStat> stats = qpmStats.computeIfAbsent(key, k -> new ConcurrentHashMap<>());

        // 初始化当前分钟的数据
        long now = System.currentTimeMillis();
        long timestamp = now - now % Duration.ofMinutes(1).toMillis();
        CacheQpmStat stat = stats.computeIfAbsent(timestamp, k -> new CacheQpmStat(cacheType, operationType, cacheName));

        // 累计 qpm 和 rt
        stat.addStat(rt);
    }

    /**
     * 统计缓存的命中数据
     */
    public void addHitStat(CacheType cacheType, String cacheName, long totalCount, long hitCount) {
        // 初始化当前缓存的数据
        String key = cacheType + "_" + cacheName;
        Map<Long, CacheHitStat> stats = hitStats.computeIfAbsent(key, k -> new ConcurrentHashMap<>());

        // 初始化当前分钟的数据
        long now = System.currentTimeMillis();
        long timestamp = now - now % Duration.ofMinutes(1).toMillis();
        CacheHitStat stat = stats.computeIfAbsent(timestamp, k -> new CacheHitStat(cacheType, cacheName));

        // 累计 totalCount 和 hitCount
        stat.addStat(totalCount, hitCount);
    }

    /**
     * 打印统计日志
     */
    private void printStatLogs() {
        long now = System.currentTimeMillis();
        long timestamp = now - now % Duration.ofMinutes(1).toMillis();

        // qpm/rt 统计
        for (Map<Long, CacheQpmStat> stats : qpmStats.values()) {
            // 查找已到期的记录
            List<Long> timeUpKeys = new ArrayList<>();
            for (Entry<Long, CacheQpmStat> entry : stats.entrySet()) {
                if (entry.getKey() < timestamp) {
                    timeUpKeys.add(entry.getKey());
                }
            }

            // 移除过期记录，并打印日志
            for (Long key : timeUpKeys) {
                CacheQpmStat stat = stats.remove(key);
                if (stat != null) {
                    new BizLoggerImpl("cache_stat")
                        .ap("bizType", "qpm_stat")
                        .ap("cacheType", stat.getCacheType())
                        .ap("operationType", stat.getOperationType())
                        .ap("cacheName", stat.getCacheName())
                        .ap("qpm", stat.getQpm())
                        .ap("time", new DecimalFormat("0.00").format(stat.getRt() / 1000000.0))
                        .ap("avgTime", new DecimalFormat("0.00").format(stat.getRt() / 1000000.0 / stat.getQpm()))
                        .println();
                }
            }
        }

        // 命中率统计
        for (Map<Long, CacheHitStat> stats : hitStats.values()) {
            // 查找已到期的记录
            List<Long> timeUpKeys = new ArrayList<>();
            for (Entry<Long, CacheHitStat> entry : stats.entrySet()) {
                if (entry.getKey() < timestamp) {
                    timeUpKeys.add(entry.getKey());
                }
            }

            // 移除过期记录，并打印日志
            for (Long key : timeUpKeys) {
                CacheHitStat stat = stats.remove(key);
                if (stat != null) {
                    new BizLoggerImpl("cache_stat")
                        .ap("bizType", "hit_stat")
                        .ap("cacheType", stat.getCacheType())
                        .ap("cacheName", stat.getCacheName())
                        .ap("totalCount", stat.getTotalCount())
                        .ap("hitCount", stat.getHitCount())
                        .ap("hitRate", new DecimalFormat("0.00").format(stat.getHitCount() * 100.0 / stat.getTotalCount()))
                        .println();
                }
            }
        }
    }
}
