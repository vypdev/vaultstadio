/**
 * API Result wrapper
 *
 * Sealed class for type-safe API call results.
 */

package com.vaultstadio.app.data.network

/**
 * Result wrapper for API calls.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: String, val message: String) : ApiResult<Nothing>()
    data class NetworkError(val message: String) : ApiResult<Nothing>()

    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error || this is NetworkError

    fun getOrNull(): T? = (this as? Success)?.data

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw ApiException(code, message)
        is NetworkError -> throw NetworkException(message)
    }

    inline fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is NetworkError -> this
    }

    inline fun <R> flatMap(transform: (T) -> ApiResult<R>): ApiResult<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
        is NetworkError -> this
    }

    inline fun onSuccess(action: (T) -> Unit): ApiResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (String, String) -> Unit): ApiResult<T> {
        if (this is Error) action(code, message)
        return this
    }

    inline fun onNetworkError(action: (String) -> Unit): ApiResult<T> {
        if (this is NetworkError) action(message)
        return this
    }

    inline fun fold(
        onSuccess: (T) -> Unit,
        onError: (String, String) -> Unit,
        onNetworkError: (String) -> Unit,
    ) {
        when (this) {
            is Success -> onSuccess(data)
            is Error -> onError(code, message)
            is NetworkError -> onNetworkError(message)
        }
    }

    companion object {
        fun <T> success(data: T): ApiResult<T> = Success(data)
        fun <T> error(code: String, message: String): ApiResult<T> = Error(code, message)
        fun <T> networkError(message: String): ApiResult<T> = NetworkError(message)
    }
}

/**
 * Exception for API errors.
 */
class ApiException(val code: String, override val message: String) : Exception(message)

/**
 * Exception for network errors.
 */
class NetworkException(override val message: String) : Exception(message)
