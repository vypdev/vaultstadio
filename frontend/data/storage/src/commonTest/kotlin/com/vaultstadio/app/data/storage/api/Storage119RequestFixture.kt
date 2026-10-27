package com.vaultstadio.app.data.storage.api

import com.vaultstadio.app.data.network.ApiResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal suspend fun storage119Request(
    method: HttpMethod,
    path: String,
    response: String = STORAGE119_ITEM,
    query: Map<String, String> = emptyMap(),
    body: String? = null,
    operation: suspend (StorageApi) -> ApiResult<*>,
) {
    var calls = 0
    val engine = MockEngine { request ->
        calls++
        assertEquals("audit.invalid", request.url.host)
        assertEquals(method, request.method)
        assertEquals(path, request.url.encodedPath)
        assertEquals(query, request.url.parameters.names().associateWith { request.url.parameters[it]!! })
        assertEquals("Bearer ephemeral119", request.headers[HttpHeaders.Authorization])
        if (body != null) {
            assertEquals(Json.parseToJsonElement(body), Json.parseToJsonElement(request.body.toByteArray().decodeToString()))
        }
        respond(response, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }
    withStorage119Client(engine) { client -> assertIs<ApiResult.Success<*>>(operation(StorageApi(client))) }
    assertEquals(1, calls)
}
