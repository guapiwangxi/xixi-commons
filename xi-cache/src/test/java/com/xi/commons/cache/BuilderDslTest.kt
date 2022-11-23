package com.xi.commons.cache

import com.xi.commons.cache.CacheType.*
import com.xi.commons.cache.builder.multiLevelCacheManager
import com.taobao.eagleeye.redis.clients.jedis.JedisPool
import com.taobao.eagleeye.redis.clients.jedis.JedisPoolConfig
import java.net.URI

val cacheManager = multiLevelCacheManager {
    clients {
        guava()
        redis(JedisPool(JedisPoolConfig(), URI.create("redis://127.0.0.1:6379/0")))
    }
    caches {
        cache("name", ver = "v2", expire = 5.min, reload = 3.min, l1 = GUAVA, l2 = REDIS)
    }
}

fun main() {
    cacheManager.put("name", "key2", null)

    val value: String? = cacheManager.get("name", "key2")
    println(value)
}
