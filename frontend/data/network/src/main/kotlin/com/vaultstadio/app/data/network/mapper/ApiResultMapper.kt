/**
 * Maps data-layer ApiResult to domain Result at the data→domain boundary.
 *
 * Repository implementations use this so that they return Result&lt;DomainModel&gt;
 * to use cases; the API layer keeps using ApiResult&lt;DTO&gt; or ApiResult&lt;T&gt;.
 */

package com.vaultstadio.app.data.network.mapper

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.result.Result

/**
 * Converts ApiResult to domain Result when the inner type is already the domain type
 * (e.g. after services have done DTO→domain mapping).
 */
fun <T> ApiResult<T>.toResult(): Result<T> = when (this) {
    is ApiResult.Success -> Result.Success(data)
    is ApiResult.Error -> Result.Error(code, message)
    is ApiResult.NetworkError -> Result.NetworkError(message)
}

/**
 * Converts ApiResult&lt;D&gt; to Result&lt;M&gt; by transforming the success payload
 * (e.g. ApiResult&lt;MyClassDTO&gt; → Result&lt;MyClass&gt; with transform = { it.toDomain() }).
 */
fun <D, M> ApiResult<D>.toResult(transform: (D) -> M): Result<M> = when (this) {
    is ApiResult.Success -> Result.Success(transform(data))
    is ApiResult.Error -> Result.Error(code, message)
    is ApiResult.NetworkError -> Result.NetworkError(message)
}
