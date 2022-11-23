package com.xi.commons.cache.builder

import com.alibaba.global.landlord.model.TenantLocal
import com.alibaba.global.satellite.proxy.tair.wrapper.TairManagerWrapper
import com.xi.commons.cache.CacheConfig
import com.xi.commons.cache.CacheType
import com.xi.commons.cache.ICacheManager
import com.xi.commons.cache.client.GuavaCacheClient
import com.xi.commons.cache.client.LandlordRedisCacheClient
import com.xi.commons.cache.client.LandlordTairCacheClient
import com.xi.commons.cache.client.RedisCacheClient
import com.xi.commons.cache.client.TairCacheClient
import com.xi.commons.cache.impl.MultiLevelCacheManager
import com.taobao.eagleeye.redis.clients.jedis.JedisPool
import com.taobao.tair.TairManager
import java.time.Duration

/**
 * 创建并配置一个缓存管理器
 */
fun multiLevelCacheManager(configure: CacheManagerBuilder.() -> Unit): ICacheManager {
    val cacheManager = MultiLevelCacheManager()
    CacheManagerBuilder(cacheManager).configure()
    cacheManager.initialize()
    return cacheManager
}

/**
 * 缓存构建 DSL marker
 */
@DslMarker
annotation class CacheBuilderDsl

/**
 * 缓存配置 builder
 */
@CacheBuilderDsl
class CacheManagerBuilder(private val cacheManager: ICacheManager) {

    /**
     * 配置缓存客户端
     */
    fun clients(configure: CacheClientsBuilder.() -> Unit) {
        CacheClientsBuilder(cacheManager).configure()
    }

    /**
     * 配置缓存
     */
    fun caches(configure: CacheConfigsBuilder.() -> Unit) {
        CacheConfigsBuilder(cacheManager).configure()
    }
}

/**
 * 缓存客户端配置 builder
 */
@CacheBuilderDsl
class CacheClientsBuilder(private val cacheManager: ICacheManager) {

    /**
     * 配置 guava 缓存客户端
     */
    fun guava(enableTenantPrefix: Boolean = true) {
        cacheManager.addClient(GuavaCacheClient(enableTenantPrefix))
    }

    /**
     * 配置 tair 缓存客户端
     */
    fun tair(tairManager: TairManager, namespace: Int, enableEnvPrefix: Boolean = true, enableTenantPrefix: Boolean = true) {
        cacheManager.addClient(TairCacheClient(tairManager, namespace, enableEnvPrefix, enableTenantPrefix))
    }

    /**
     * 配置多租户 tair 缓存客户端
     */
    fun landlordTair(tairManagerWrapper: TairManagerWrapper, enableEnvPrefix: Boolean = true, enableTenantPrefix: Boolean = true) {
        cacheManager.addClient(LandlordTairCacheClient(tairManagerWrapper, enableEnvPrefix, enableTenantPrefix))
    }

    /**
     * 配置 redis 缓存客户端
     */
    fun redis(jedisPool: JedisPool, enableEnvPrefix: Boolean = true, enableTenantPrefix: Boolean = true) {
        cacheManager.addClient(RedisCacheClient(jedisPool, enableEnvPrefix, enableTenantPrefix))
    }

    /**
     * 配置多租户 redis 缓存客户端
     */
    fun landlordRedis(jedisPool: TenantLocal<JedisPool>, enableEnvPrefix: Boolean = true, enableTenantPrefix: Boolean = true) {
        cacheManager.addClient(LandlordRedisCacheClient(jedisPool, enableEnvPrefix, enableTenantPrefix))
    }
}

/**
 * 缓存配置 builder
 */
@CacheBuilderDsl
class CacheConfigsBuilder(private val cacheManager: ICacheManager) {

    /**
     * 配置缓存
     */
    fun cache(name: String, ver: String? = null, expire: Duration, reload: Duration? = null, l1: CacheType, l2: CacheType? = null) {
        val config = CacheConfig.builder()
            .name(name)
            .version(ver)
            .expireTime(expire)
            .reloadTime(reload)
            .level1(l1)
            .level2(l2)
            .build()

        cacheManager.addConfig(config)
    }

    /**
     * 支持使用 5.milli 的语法创建 Duration 对象
     */
    val Int.milli: Duration get() = Duration.ofMillis(this.toLong())

    /**
     * 支持使用 5.sec 的语法创建 Duration 对象
     */
    val Int.sec: Duration get() = Duration.ofSeconds(this.toLong())

    /**
     * 支持使用 5.min 的语法创建 Duration 对象
     */
    val Int.min: Duration get() = Duration.ofMinutes(this.toLong())

    /**
     * 支持使用 5.hour 的语法创建 Duration 对象
     */
    val Int.hour: Duration get() = Duration.ofHours(this.toLong())

    /**
     * 支持使用 5.day 的语法创建 Duration 对象
     */
    val Int.day: Duration get() = Duration.ofDays(this.toLong())
}
