package com.niku.network.util

/**
 * Обертка результатов выполнения сетевых запросов.
 */
sealed interface NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : NetworkResult<Nothing>
}
