/**
 * Base API class
 *
 * Provides common HTTP request functionality for all API classes.
 */

package com.vaultstadio.app.data.network

import com.vaultstadio.app.data.network.dto.common.ApiResponseDTO
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.isSuccess

/**
 * Base class for API implementations.
 */
abstract class BaseApi(protected val client: HttpClient) {

    protected suspend inline fun <reified T> get(
        path: String,
        params: Map<String, String> = emptyMap(),
    ): ApiResult<T> = requestNoBody(HttpMethod.Get, path, params)

    protected suspend inline fun <reified T, reified B> post(
        path: String,
        body: B,
    ): ApiResult<T> = request(HttpMethod.Post, path, body)

    protected suspend inline fun <reified T> postNoBody(
        path: String,
    ): ApiResult<T> = requestNoBody(HttpMethod.Post, path)

    protected suspend inline fun <reified T, reified B> patch(
        path: String,
        body: B,
    ): ApiResult<T> = request(HttpMethod.Patch, path, body)

    protected suspend inline fun <reified T> delete(
        path: String,
    ): ApiResult<T> = requestNoBody(HttpMethod.Delete, path)

    protected suspend inline fun <reified T> deleteWithParams(
        path: String,
        params: Map<String, String> = emptyMap(),
    ): ApiResult<T> = requestNoBody(HttpMethod.Delete, path, params)

    protected suspend inline fun <reified T> postForm(
        path: String,
        formParams: Map<String, String>,
    ): ApiResult<T> {
        return try {
            val response = client.request(path) {
                method = HttpMethod.Post
                setBody(
                    FormDataContent(
                        Parameters.build {
                            formParams.forEach { (key, value) -> append(key, value) }
                        },
                    ),
                )
            }

            if (response.status.isSuccess()) {
                val apiResponse = response.body<ApiResponseDTO<T>>()
                if (apiResponse.success && apiResponse.data != null) {
                    ApiResult.Success(apiResponse.data)
                } else {
                    ApiResult.Error(
                        apiResponse.error?.code ?: "UNKNOWN_ERROR",
                        apiResponse.error?.message ?: "Unknown error",
                    )
                }
            } else {
                handleErrorResponse(response)
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e.message ?: "Network error")
        }
    }

    protected suspend inline fun <reified T> requestNoBody(
        method: HttpMethod,
        path: String,
        params: Map<String, String> = emptyMap(),
    ): ApiResult<T> {
        return try {
            val response = client.request(path) {
                this.method = method
                params.forEach { (key, value) ->
                    parameter(key, value)
                }
            }

            if (response.status.isSuccess()) {
                val apiResponse = response.body<ApiResponseDTO<T>>()
                if (apiResponse.success && apiResponse.data != null) {
                    ApiResult.Success(apiResponse.data)
                } else if (apiResponse.success) {
                    @Suppress("UNCHECKED_CAST")
                    ApiResult.Success(Unit as T)
                } else {
                    ApiResult.Error(
                        apiResponse.error?.code ?: "UNKNOWN_ERROR",
                        apiResponse.error?.message ?: "Unknown error",
                    )
                }
            } else {
                handleErrorResponse(response)
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e.message ?: "Network error")
        }
    }

    protected suspend inline fun <reified T, reified B> request(
        method: HttpMethod,
        path: String,
        body: B? = null,
        params: Map<String, String> = emptyMap(),
    ): ApiResult<T> {
        return try {
            val response = client.request(path) {
                this.method = method
                params.forEach { (key, value) ->
                    parameter(key, value)
                }
                body?.let {
                    setBody(it)
                }
            }

            if (response.status.isSuccess()) {
                val apiResponse = response.body<ApiResponseDTO<T>>()
                if (apiResponse.success && apiResponse.data != null) {
                    ApiResult.Success(apiResponse.data)
                } else {
                    ApiResult.Error(
                        apiResponse.error?.code ?: "UNKNOWN_ERROR",
                        apiResponse.error?.message ?: "Unknown error",
                    )
                }
            } else {
                handleErrorResponse(response)
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e.message ?: "Network error")
        }
    }

    protected suspend inline fun <reified T> handleErrorResponse(
        response: HttpResponse,
    ): ApiResult<T> {
        return try {
            val errorResponse = response.body<ApiResponseDTO<Unit>>()
            ApiResult.Error(
                errorResponse.error?.code ?: "HTTP_${response.status.value}",
                errorResponse.error?.message ?: response.status.description,
            )
        } catch (e: Exception) {
            ApiResult.Error("HTTP_${response.status.value}", response.status.description)
        }
    }
}
