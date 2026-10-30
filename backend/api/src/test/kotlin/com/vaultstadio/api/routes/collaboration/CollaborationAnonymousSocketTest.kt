package com.vaultstadio.api.routes.collaboration

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.net.Socket
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import kotlin.test.assertEquals

/** A real Netty handshake: the test-host engine cannot represent a rejected WebSocket upgrade. */
class CollaborationAnonymousSocketTest {
    @Test
    fun `C89-WS anonymous native handshake returns 401 without registration or state effects`() = runBlocking {
        val f = CollaborationIntegrationFixture()
        f.start()
        try {
            f.reset()
            val session = UUID.randomUUID().toString()
            val before = f.snapshot()
            val server = embeddedServer(Netty, host = "127.0.0.1", port = 0) { f.install(this) }
            try {
                server.start(wait = false)
                val port = server.engine.resolvedConnectors().single().port
                val nonce = ByteArray(16).also { SecureRandom().nextBytes(it) }
                val key = Base64.getEncoder().encodeToString(nonce)
                Socket("127.0.0.1", port).use { socket ->
                    socket.soTimeout = 5_000
                    val request = "GET /api/v1/collaboration/ws/$session HTTP/1.1\r\n" +
                        "Host: 127.0.0.1:$port\r\nConnection: Upgrade\r\nUpgrade: websocket\r\n" +
                        "Sec-WebSocket-Version: 13\r\nSec-WebSocket-Key: $key\r\n\r\n"
                    socket.getOutputStream().write(request.toByteArray(Charsets.US_ASCII))
                    assertEquals("HTTP/1.1 401 Unauthorized", socket.getInputStream().bufferedReader().readLine())
                }
                assertEquals(0, CollaborationWebSocketManager.getParticipantCount(session))
                assertEquals(before, f.snapshot())
            } finally {
                server.stop(gracePeriodMillis = 0, timeoutMillis = 1_000)
            }
        } finally {
            f.close()
        }
    }
}
