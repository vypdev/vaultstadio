package com.vaultstadio.app.data.storage.api

import com.sun.net.httpserver.HttpServer
import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.network.HttpClientFactory
import com.vaultstadio.app.data.network.TokenProvider
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Real default desktop engine and real loopback HTTP; no MockEngine or external endpoint. */
class Storage119LoopbackTest {
    @Test fun sto119DefaultEngineSendsActualMultipartContentTypeAndOriginalBytes() = runBlocking {
        val capturedType = AtomicReference<String>()
        val capturedBody = AtomicReference<ByteArray>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val bytes = byteArrayOf(0, 1, 127, -1)
        server.createContext("/api/v1/storage/upload") { exchange ->
            capturedType.set(exchange.requestHeaders.getFirst("Content-Type"))
            capturedBody.set(exchange.requestBody.use { it.readAllBytes() })
            val response = STORAGE119_ITEM.toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.start()
        try {
            HttpClientFactory.create(ApiClientConfig("http://127.0.0.1:${server.address.port}", 3_000), TokenProvider { null }).use { client ->
                val result = StorageApi(client).uploadFile("audit119.bin", bytes, "application/octet-stream")
                assertEquals("file119", result.getOrNull()?.id, result.toString())
            }
            val contentType = assertNotNull(capturedType.get())
            assertTrue(contentType.startsWith("multipart/form-data;"), contentType)
            val boundary = contentType.substringAfter("boundary=").trim('"')
            val body = assertNotNull(capturedBody.get())
            assertTrue(body.decodeToString().contains(boundary))
            assertTrue(body.asList().windowed(bytes.size).any { it == bytes.asList() })
        } finally {
            server.stop(0)
        }
    }
}
