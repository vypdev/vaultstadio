package com.vaultstadio.app.data.storage.api

import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.HttpClientFactory
import com.vaultstadio.app.data.network.TokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Storage116PrefixTransferTest {
    private val payload = byteArrayOf(0, 1, -1, -128, 127)

    @Test fun api116MultipartAndChunkBodiesSurviveRebasing() = runTest {
        var count = 0
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("Bearer transfer-session", request.headers[HttpHeaders.Authorization])
            assertTrue(request.body.contentType!!.match(ContentType.MultiPart.FormData))
            val body = request.body.toByteArray()
            assertTrue(body.asList().windowed(payload.size).any { it == payload.asList() })
            if (count++ == 0) {
                assertEquals("/gateway/api/v1/storage/upload", request.url.encodedPath)
                assertTrue(body.decodeToString().contains("filename=\"é.txt\""))
                assertTrue(body.decodeToString().contains("parent116"))
                respond(STORAGE119_ITEM, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                assertEquals("/gateway/api/v1/storage/upload/upload116/chunk/3", request.url.encodedPath)
                assertTrue(body.decodeToString().contains("chunk_3"))
                respond(STORAGE119_CHUNK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        withClient(engine) { client ->
            assertIs<ApiResult.Success<*>>(
                StorageApi(client).uploadFile("é.txt", payload, "application/octet-stream", "parent116"),
            )
            assertIs<ApiResult.Success<*>>(StorageApi(client).uploadChunk("upload116", 3, payload))
        }
        assertEquals(2, count)
    }

    @Test fun api116BinaryAndArchiveRequestsRetainBytesAndExactRoutes() = runTest {
        var count = 0
        val engine = MockEngine { request ->
            assertEquals("Bearer transfer-session", request.headers[HttpHeaders.Authorization])
            if (count++ == 0) {
                assertEquals(HttpMethod.Get, request.method)
                assertEquals("/gateway/api/v1/storage/download/a%2Fb%3F%20%C3%A9", request.url.encodedPath)
            } else {
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("/gateway/api/v1/storage/batch/download-zip", request.url.encodedPath)
                assertEquals(
                    Json.parseToJsonElement("{\"itemIds\":[\"a/b? é\",\"second\"]}"),
                    Json.parseToJsonElement(request.body.toByteArray().decodeToString()),
                )
            }
            respond(payload, headers = headersOf(HttpHeaders.ContentType, "application/octet-stream"))
        }
        withClient(engine) { client ->
            assertContentEquals(payload, StorageApi(client).downloadFile("a/b? é").getOrNull())
            assertContentEquals(payload, StorageApi(client).downloadArchive(listOf("a/b? é", "second")).getOrNull())
        }
        assertEquals(2, count)
    }

    @Test fun api116DeniedBinaryDoesNotBecomeAnEmptySuccessfulFile() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/gateway/api/v1/storage/download/id", request.url.encodedPath)
            respond("denied", HttpStatusCode.Forbidden)
        }
        withClient(engine) { assertIs<ApiResult.Error>(StorageApi(it).downloadFile("id")) }
    }
    private suspend fun withClient(engine: MockEngine, action: suspend (HttpClient) -> Unit) {
        val client = HttpClientFactory.create(
            ApiClientConfig("https://viewer.invalid/gateway/api"),
            TokenProvider {
                "transfer-session"
            },
            engine,
        )
        try {
            action(client)
        } finally {
            client.close()
            engine.close()
        }
    }
}
