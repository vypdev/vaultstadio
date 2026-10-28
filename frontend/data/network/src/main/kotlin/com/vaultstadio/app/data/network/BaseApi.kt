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

/** Reified wrappers supply codecs; one executed path owns cancellation and response semantics. */
abstract class BaseApi(protected val client: HttpClient) {
    protected suspend inline fun <reified T> get(path: String, params: Map<String, String> = emptyMap()): ApiResult<T> =
        requestNoBody(HttpMethod.Get, path, params)
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
    protected suspend inline fun <reified T> delete(path: String): ApiResult<T> = requestNoBody(HttpMethod.Delete, path)
    protected suspend inline fun <reified T> deleteWithParams(
        path: String,
        params: Map<String, String> = emptyMap(),
    ): ApiResult<T> =
        requestNoBody(HttpMethod.Delete, path, params)

    protected suspend inline fun <reified T> postForm(
        path: String,
        formParams: Map<String, String>,
    ): ApiResult<T> {
        val transport = client
        return executeRequest(
            request = {
                transport.request(path) {
                    method = HttpMethod.Post
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                formParams.forEach { (key, value) -> append(key, value) }
                            },
                        ),
                    )
                }
            },
            decode = { it.body<ApiResponseDTO<T>>() },
            unitResult = T::class == Unit::class,
        )
    }

    protected suspend inline fun <reified T> requestNoBody(
        method: HttpMethod,
        path: String,
        params: Map<String, String> = emptyMap(),
    ): ApiResult<T> {
        val transport = client
        return executeRequest(
            request = {
                transport.request(path) {
                    this.method = method
                    params.forEach { (key, value) -> parameter(key, value) }
                }
            },
            decode = { it.body<ApiResponseDTO<T>>() },
            unitResult = T::class == Unit::class,
        )
    }

    protected suspend inline fun <reified T, reified B> request(
        method: HttpMethod,
        path: String,
        body: B? = null,
        params: Map<String, String> = emptyMap(),
    ): ApiResult<T> {
        val transport = client
        return executeRequest(
            request = {
                transport.request(path) {
                    this.method = method
                    params.forEach { (key, value) -> parameter(key, value) }
                    body?.let { setBody(it) }
                }
            },
            decode = { it.body<ApiResponseDTO<T>>() },
            unitResult = T::class == Unit::class,
        )
    }

    @PublishedApi
    internal suspend fun <T> executeRequest(
        request: suspend () -> HttpResponse,
        decode: suspend (HttpResponse) -> ApiResponseDTO<T>,
        unitResult: Boolean,
    ): ApiResult<T> = try {
        val response = request()
        if (!response.status.isSuccess()) {
            handleErrorResponse(response)
        } else {
            val envelope = decode(response)
            val data = envelope.data
            when {
                !envelope.success -> ApiResult.Error(
                    envelope.error?.code ?: "UNKNOWN_ERROR",
                    envelope.error?.message ?: "Unknown error",
                )
                data != null -> ApiResult.Success(data)
                unitResult -> {
                    @Suppress("UNCHECKED_CAST")
                    ApiResult.Success(Unit as T)
                }
                else -> ApiResult.Error("UNKNOWN_ERROR", "Missing response data")
            }
        }
    } catch (error: Throwable) {
        rethrowTransportCancellation(error)
        ApiResult.NetworkError(error.message ?: "Network error")
    }

    private suspend fun handleErrorResponse(response: HttpResponse): ApiResult<Nothing> = try {
        val envelope = response.body<ApiResponseDTO<Unit>>()
        ApiResult.Error(
            envelope.error?.code ?: "HTTP_${response.status.value}",
            envelope.error?.message ?: response.status.description,
        )
    } catch (error: Throwable) {
        rethrowTransportCancellation(error)
        ApiResult.Error("HTTP_${response.status.value}", response.status.description)
    }
}
