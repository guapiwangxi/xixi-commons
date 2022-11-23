package com.xi.commons.util

import org.springframework.context.ApplicationContext
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Created by vince at Nov 02, 2020.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class SpringUtils(applicationContext: ApplicationContext) {

    init {
        Companion.applicationContext = applicationContext
    }

    companion object {
        private lateinit var applicationContext: ApplicationContext

        @JvmStatic
        fun getApplicationContext(): ApplicationContext = applicationContext

        @JvmStatic
        fun <T> getBean(cls: Class<T>): T = applicationContext.getBean(cls)

        @JvmStatic
        fun <T> getBean(name: String, cls: Class<T>): T = applicationContext.getBean(name, cls)

        @JvmStatic
        inline fun <reified T> getBean(): T = getBean(T::class.java)

        @JvmStatic
        inline fun <reified T> getBean(name: String): T = getBean(name, T::class.java)

        @JvmStatic
        fun <T> getBeansOfType(cls: Class<T>): Collection<T> = applicationContext.getBeansOfType(cls).map { it.value }

        @JvmStatic
        inline fun <reified T> getBeansOfType(): Collection<T> = getBeansOfType(T::class.java)
    }
}