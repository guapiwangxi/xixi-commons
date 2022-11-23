package com.xi.commons.util

/**
 * Created by vince at Dec 02, 2020.
 */
object StringUtils {

    fun String?.orElse(defaultString: String): String {
        if (this == null || this.isBlank()) {
            return defaultString
        } else {
            return this
        }
    }

    fun String?.isNotNullOrBlank(): Boolean {
        return this != null && this.isNotBlank()
    }
}