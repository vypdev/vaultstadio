package com.vaultstadio.api.routes.collaboration

import com.vaultstadio.domain.storage.model.Visibility
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CollaborationSocketHandshakeTest {
    private val f = CollaborationIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @AfterAll fun close() = f.close()

    @ParameterizedTest(name = "C89-WS handshake {0} {1}")
    @MethodSource("cases")
    fun `socket metadata and registration require canonical owner and own participant`(
        visibility: Visibility,
        actor: String,
    ) {
        f.reset(visibility)
        testApplication {
            application { f.install(this) }
            val wsClient = createClient { install(WebSockets) }
            startApplication()
            val s = CollaborationSocketFixture(f, wsClient)
            s.prepare()
            val before = f.snapshot()
            try {
                val socket = s.open(actor)
                assertEquals(0, CollaborationWebSocketManager.getParticipantCount(s.session))
                if (actor == "owner") {
                    socket.sendMessage(WebSocketMessage("ping"))
                    assertEquals("pong", socket.packet().type())
                    socket.sendMessage(WebSocketMessage("join", participantId = s.first))
                    assertEquals("joined", socket.packet().type())
                    assertEquals(1, CollaborationWebSocketManager.getParticipantCount(s.session))
                } else {
                    socket.policyClosed()
                    assertEquals(before, f.snapshot())
                    assertEquals(0, CollaborationWebSocketManager.getParticipantCount(s.session))
                }
            } finally {
                s.close()
            }
        }
    }

    companion object {
        @JvmStatic fun cases(): List<Arguments> = Visibility.entries.flatMap { visibility ->
            listOf("owner", "unrelated", "shared", "admin").map { Arguments.of(visibility, it) }
        }
    }
}
