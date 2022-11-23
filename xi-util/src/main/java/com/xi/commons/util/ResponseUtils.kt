package com.xi.commons.util

import com.alibaba.ecommerce.error.ErrorCode
import com.alibaba.ecommerce.module.Response

/**
 * Created by vince at Nov 20, 2020.
 */
object ResponseUtils {

    /**
     * 把 [ZalResponse] 转换成 [Response]
     */
    @JvmStatic
    fun <T> from(response: ZalResponse<T>): Response<T> {
        if (response.isSuccess) {
            return Response.success(response.module)
        } else {
            return Response.failed(ErrorCode.frontEndCode(response.errorCode.key, response.errorCode.displayMessage), response.module)
        }
    }

    /**
     * 如果成功，把 module 转换成另一个结果，如果失败，传递错误信息
     */
    @JvmStatic
    inline fun <R, T> Response<T>.map(transform: (module: T) -> R): Response<R> {
        if (isSuccess) {
            return Response.success(transform(module))
        } else {
            return Response.failed(errorCode)
        }
    }

    /**
     * 与 map 方法功能类似，但是在失败时也执行 module 转换
     */
    @JvmStatic
    inline fun <R, T> Response<T>.mapAnyway(transform: (module: T) -> R): Response<R> {
        if (isSuccess) {
            return Response.success(transform(module))
        } else {
            return Response.failed(errorCode, transform(module))
        }
    }

    /**
     * 如果成功，返回 module，否则返回 null
     */
    @JvmStatic
    fun <T> Response<T>.getOrNull(): T? {
        if (isSuccess) {
            return module
        } else {
            return null
        }
    }

    /**
     * 如果成功，返回 module，否则返回默认值
     */
    @JvmStatic
    fun <R, T : R> Response<T>.getOrDefault(defaultValue: R): R {
        if (isSuccess) {
            return module
        } else {
            return defaultValue
        }
    }

    /**
     * 如果成功，返回 module，否则 onFailure 函数返回的默认值
     */
    @JvmStatic
    inline fun <R, T : R> Response<T>.getOrElse(onFailure: (errorCode: ErrorCode) -> R): R {
        if (isSuccess) {
            return module
        } else {
            return onFailure(errorCode)
        }
    }

    /**
     * 如果成功，返回 module，否则返回抛出相同错误码的 [ZalException]
     */
    @JvmStatic
    fun <T> Response<T>.getOrThrow(): T {
        if (isSuccess) {
            return module
        } else {
            throw ZalException(errorCode.key, errorCode.displayMessage)
        }
    }
}