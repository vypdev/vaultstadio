package com.vaultstadio.app.data.storage.api

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class Storage78RouteTest {
    @Test fun transfer78UsesTheRegisteredAuthenticatedItemRoute() = runTest {
        val bytes = byteArrayOf(0, -1, 127, 4)
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/api/v1/storage/download/file78", request.url.encodedPath)
            assertEquals("Bearer ephemeral119", request.headers[HttpHeaders.Authorization])
            assertFalse(request.url.toString().contains("ephemeral119"))
            respond(bytes, headers = headersOf(HttpHeaders.ContentType, "application/octet-stream"))
        }
        withStorage119Client(engine) { client ->
            assertContentEquals(bytes, StorageApi(client).downloadFile("file78").getOrNull())
        }
    }
}
