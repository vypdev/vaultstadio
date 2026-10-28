package com.vaultstadio.app.data.storage.api

import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.network.HttpClientFactory
import com.vaultstadio.app.data.network.TokenProvider
import com.vaultstadio.app.data.network.apiV1Location
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class Storage78LocationTest {
    @Test fun transfer78ArchiveUsesAuthenticatedPostWithExactSelection() = runTest {
        val bytes = byteArrayOf(80, 75, 3, 4, -1)
        var calls = 0
        val engine = MockEngine { request ->
            calls++
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/api/v1/storage/batch/download-zip", request.url.encodedPath)
            assertEquals("Bearer ephemeral119", request.headers[HttpHeaders.Authorization])
            assertEquals(
                Json.parseToJsonElement("{\"itemIds\":[\"one\",\"two\"]}"),
                Json.parseToJsonElement(request.body.toByteArray().decodeToString()),
            )
            assertFalse(request.url.toString().contains("ephemeral119"))
            respond(bytes, headers = headersOf(HttpHeaders.ContentType, "application/zip"))
        }
        withStorage119Client(engine) { client ->
            assertContentEquals(bytes, StorageDownloadApi(client).archive(listOf("one", "two")).getOrNull())
        }
        assertEquals(1, calls)
    }

    @Test fun transfer78OriginBaseGetsExactlyOneApiPrefix() = prefix("https://audit.invalid")

    @Test fun transfer78ApiBaseGetsExactlyOneApiPrefix() = prefix("https://audit.invalid/api")

    @Test fun transfer78VersionedBaseGetsExactlyOneApiPrefix() = prefix("https://audit.invalid/api/v1/")

    @Test fun transfer78DownloadIdIsEncodedAsOnePathSegment() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/storage/download/one%2Ftwo%3Ftoken=bad%23fragment", request.url.encodedPath)
            assertTrueQueryEmpty(request.url)
            respond(byteArrayOf(1))
        }
        withStorage119Client(engine) { client -> StorageApi(client).downloadFile("one/two?token=bad#fragment") }
    }

    @Test fun transfer78RotatedTokenIsReadForEveryDownload() = runTest {
        var token = "ephemeral-first"
        val headers = mutableListOf<String?>()
        val engine = MockEngine { request ->
            headers += request.headers[HttpHeaders.Authorization]
            assertTrueQueryEmpty(request.url)
            respond(byteArrayOf(1))
        }
        val client = HttpClientFactory.create(
            ApiClientConfig("https://audit.invalid/api"),
            TokenProvider {
                token
            },
            engine,
        )
        try {
            StorageApi(client).downloadFile("first")
            token = "ephemeral-second"
            StorageApi(client).downloadFile("second")
            assertEquals(listOf<String?>("Bearer ephemeral-first", "Bearer ephemeral-second"), headers)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun transfer78PublicLocationDropsInheritedUserPasswordQueryAndFragment() {
        val base = URLBuilder("https://audit.invalid/api").apply {
            user = "fixture-user"
            password = "fixture-password"
            parameters.append("token", "fixture-query")
            fragment = "fixture-fragment"
        }.buildString()
        val url = Url(apiV1Location(base, listOf("storage", "download", "id")))
        assertEquals("https://audit.invalid/api/v1/storage/download/id", url.toString())
    }

    @Test fun transfer78LocationRejectsTraversalSegments() {
        assertFailsWith<IllegalArgumentException> { apiV1Location("https://audit.invalid", listOf("storage", "..")) }
    }

    private fun prefix(base: String) = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/storage/download/id", request.url.encodedPath)
            assertTrueQueryEmpty(request.url)
            respond(byteArrayOf(1))
        }
        val client = HttpClientFactory.create(ApiClientConfig(base), TokenProvider { "ephemeral-prefix" }, engine)
        try {
            assertContentEquals(byteArrayOf(1), StorageApi(client).downloadFile("id").getOrNull())
        } finally {
            client.close()
            engine.close()
        }
    }

    private fun assertTrueQueryEmpty(url: Url) = assertEquals(emptySet(), url.parameters.names())
}
