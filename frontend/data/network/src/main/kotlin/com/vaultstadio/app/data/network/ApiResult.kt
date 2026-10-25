/**
 * Result type for the data/API layer.
 *
 * Used by BaseApi and services; carries API response or error. At the boundary
 * (repository implementations) this is mapped to domain.result.Result so that
 * domain only sees Result&lt;DomainModel&gt;, never ApiResult or DTOs.
 */

package com.vaultstadio.app.data.network

/**
 * API-layer result: success data, API error (code + message), or network error.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: String, val message: String) : ApiResult<Nothing>()
    data class NetworkError(val message: String) : ApiResult<Nothing>()

    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error || this is NetworkError

    fun getOrNull(): T? = (this as? Success)?.data

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

    companion object {
        fun <T> success(data: T): ApiResult<T> = Success(data)
        fun <T> error(code: String, message: String): ApiResult<T> = Error(code, message)
        fun <T> networkError(message: String): ApiResult<T> = NetworkError(message)
    }
}
