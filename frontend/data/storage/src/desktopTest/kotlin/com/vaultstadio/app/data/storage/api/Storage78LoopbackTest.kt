package com.vaultstadio.app.data.storage.api

import com.sun.net.httpserver.HttpServer
import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.network.HttpClientFactory
import com.vaultstadio.app.data.network.TokenProvider
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class Storage78LoopbackTest {
    @Test fun transfer78DefaultEngineReceivesActualBinaryAtTheRegisteredRoute() = runBlocking {
        val bytes = ByteArray(65537) { (it % 256).toByte() }
        val authorization = AtomicReference<String>()
        val query = AtomicReference<String?>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/v1/storage/download/file78") { exchange ->
            authorization.set(exchange.requestHeaders.getFirst("Authorization"))
            query.set(exchange.requestURI.rawQuery)
            exchange.responseHeaders.set("Content-Type", "application/octet-stream")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            HttpClientFactory.create(
                ApiClientConfig("http://127.0.0.1:${server.address.port}/api", 3000),
                TokenProvider { "owned-loopback78" },
            ).use { client ->
                assertContentEquals(bytes, StorageApi(client).downloadFile("file78").getOrNull())
            }
            assertEquals("Bearer owned-loopback78", authorization.get())
            assertEquals(null, query.get())
        } finally {
            server.stop(0)
        }
    }
}
