/**
 * Neutral result type for application operations.
 *
 * Sealed class for type-safe success/error results. Lives in domain so that
 * use cases and ViewModels do not depend on data.network (Clean Architecture).
 * The data layer has its own ApiResult type and maps to Result at the repository boundary.
 */

package com.vaultstadio.app.domain.result

/**
 * Result wrapper for application/use-case operations.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val code: String, val message: String) : Result<Nothing>()
    data class NetworkError(val message: String) : Result<Nothing>()

    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error || this is NetworkError

    fun getOrNull(): T? = (this as? Success)?.data

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw ApiException(code, message)
        is NetworkError -> throw NetworkException(message)
    }

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is NetworkError -> this
    }

    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
        is NetworkError -> this
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (String, String) -> Unit): Result<T> {
        if (this is Error) action(code, message)
        return this
    }

    inline fun onNetworkError(action: (String) -> Unit): Result<T> {
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
        fun <T> success(data: T): Result<T> = Success(data)
        fun <T> error(code: String, message: String): Result<T> = Error(code, message)
        fun <T> networkError(message: String): Result<T> = NetworkError(message)
    }
}

/**
 * Exception for application/API errors (e.g. business or remote error codes).
 */
class ApiException(val code: String, override val message: String) : Exception(message)

/**
 * Exception for network/connectivity errors.
 */
class NetworkException(override val message: String) : Exception(message)
