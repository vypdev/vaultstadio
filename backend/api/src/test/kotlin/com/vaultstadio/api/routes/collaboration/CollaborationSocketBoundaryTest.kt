package com.vaultstadio.api.routes.collaboration

import com.vaultstadio.core.domain.model.CursorPosition
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.infrastructure.persistence.entities.CollaborationOperationsTable
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CollaborationSocketBoundaryTest {
    private val f = CollaborationIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @AfterAll fun close() = f.close()

    @ParameterizedTest(name = "C89-WS {0}")
    @ValueSource(
        strings = [
            "foreign-participant",
            "other-owned-item",
            "operation-actor",
            "provider-failure-no-broadcast",
            "left-participant",
            "two-owner-connections",
            "stale-disconnect",
            "revoked-sender",
            "revoked-recipient",
            "quoted-JSON",
            "malformed-frame",
            "unknown-top-level-field",
        ],
    )
    fun `real sockets enforce identities errors delivery and lifecycle`(case: String) {
        f.reset()
        testApplication {
            application { f.install(this) }
            val wsClient = createClient {
                install(WebSockets)
            }
            startApplication()
            val s = CollaborationSocketFixture(f, wsClient)
            s.prepare()
            try {
                when (case) {
                    "foreign-participant" -> foreignParticipant(s)
                    "other-owned-item" -> wrongItem(s)
                    "operation-actor" -> operationActor(s)
                    "provider-failure-no-broadcast" -> providerFailure(s)
                    "left-participant" -> leftParticipant(s)
                    "two-owner-connections" -> twoConnections(s)
                    "stale-disconnect" -> staleDisconnect(s)
                    "revoked-sender" -> revokedSender(s)
                    "revoked-recipient" -> revokedRecipient(s)
                    "quoted-JSON" -> quotedJson(s)
                    "malformed-frame" -> malformed(s)
                    "unknown-top-level-field" -> unknownField(s)
                    else -> error("Unenumerated socket boundary")
                }
            } finally {
                s.close()
            }
        }
    }

    private suspend fun foreignParticipant(s: CollaborationSocketFixture) {
        val original = f.repository.getParticipants(s.session).fold({ throw it }, { it.first() })
        val foreign = original.copy(id = UUID.randomUUID().toString(), userId = f.storage.bob.id)
        f.repository.addParticipant(s.session, foreign).fold({ throw it }, {})
        val before = f.snapshot()
        val socket = s.open()
        socket.sendMessage(WebSocketMessage("join", participantId = foreign.id))
        assertEquals(403, socket.packet().code())
        assertEquals(0, CollaborationWebSocketManager.getParticipantCount(s.session))
        assertEquals(before, f.snapshot())
    }

    private suspend fun wrongItem(s: CollaborationSocketFixture) {
        val socket = s.joined(s.first)
        val secondItem = f.storage.seed(type = ItemType.FILE)
        val before = f.snapshot()
        socket.sendMessage(
            WebSocketMessage(
                "operation",
                itemId = secondItem.id,
                operation = OperationData("INSERT", 0, "bad", baseVersion = 1),
            ),
        )
        assertEquals(403, socket.packet().code())
        assertEquals(before, f.snapshot())
    }

    private suspend fun operationActor(s: CollaborationSocketFixture) {
        val socket = s.joined(s.first)
        socket.send(
            Frame.Text(
                """
                {"type":"operation","operation":{
                  "type":"INSERT","position":0,"text":"forged","baseVersion":1,"userId":"foreign"
                }}
                """
                    .trimIndent(),
            ),
        )
        assertEquals("ack_operation", socket.packet().type())
        assertEquals(
            "forgedowned text\n",
            f.repository.getDocumentState(f.item.id).fold({ throw it }, { checkNotNull(it).content }),
        )
        assertEquals(
            listOf(f.storage.alice.id),
            transaction {
                CollaborationOperationsTable.selectAll().map { it[CollaborationOperationsTable.userId] }
            },
        )
    }

    private suspend fun providerFailure(s: CollaborationSocketFixture) {
        val one = s.joined(s.first)
        val two = s.joined(s.second)
        assertEquals("participant_joined", one.packet().type())
        val before = f.snapshot()
        transaction { exec("ALTER TABLE document_states RENAME TO document_states_unavailable") }
        try {
            one.sendMessage(
                WebSocketMessage(
                    "operation",
                    operation = OperationData("INSERT", 0, "should not commit", baseVersion = 1),
                ),
            )
            val error = one.packet()
            assertEquals(500, error.code())
            assertEquals("Collaboration operation failed", error.getValue("message").jsonPrimitive.content)
            two.sendMessage(WebSocketMessage("ping"))
            assertEquals("pong", two.packet().type())
        } finally {
            transaction { exec("ALTER TABLE document_states_unavailable RENAME TO document_states") }
        }
        assertEquals(before, f.snapshot())
    }

    private suspend fun leftParticipant(s: CollaborationSocketFixture) {
        val socket = s.joined(s.first)
        f.service.leaveSession(s.session, s.first, f.storage.alice.id).fold({ throw it }, {})
        val before = f.snapshot()
        socket.sendMessage(WebSocketMessage("cursor_update", cursor = CursorPosition(1, 2, 3)))
        assertEquals(404, socket.packet().code())
        assertEquals(before, f.snapshot())
    }

    private suspend fun twoConnections(s: CollaborationSocketFixture) {
        val one = s.joined(s.first)
        val two = s.joined(s.second)
        assertEquals("participant_joined", one.packet().type())
        assertEquals(2, CollaborationWebSocketManager.getParticipantCount(s.session))
        one.sendMessage(WebSocketMessage("cursor_update", cursor = CursorPosition(1, 2, 3)))
        assertEquals("cursor_update", two.packet().type())
        one.sendMessage(WebSocketMessage("ping"))
        assertEquals("pong", one.packet().type())
    }

    private suspend fun staleDisconnect(s: CollaborationSocketFixture) {
        val old = s.joined(s.first)
        val current = s.joined(s.second)
        assertEquals("participant_joined", old.packet().type())
        old.close()
        assertEquals("participant_left", current.packet().type())
        assertEquals(1, CollaborationWebSocketManager.getParticipantCount(s.session))
        current.sendMessage(WebSocketMessage("ping"))
        assertEquals("pong", current.packet().type())
        assertEquals(
            listOf(s.second),
            f.repository.getParticipants(s.session).fold({
                throw it
            }, { it.map { p -> p.id } }),
        )
    }

    private suspend fun revokedSender(s: CollaborationSocketFixture) {
        val socket = s.joined(s.first)
        transaction {
            StorageItemsTable.update({ StorageItemsTable.id eq f.item.id }) { it[ownerId] = f.storage.bob.id }
        }
        val before = f.snapshot()
        socket.sendMessage(WebSocketMessage("cursor_update", cursor = CursorPosition(1, 2, 3)))
        assertEquals(403, socket.packet().code())
        assertEquals(before, f.snapshot())
    }

    private suspend fun revokedRecipient(s: CollaborationSocketFixture) {
        val one = s.joined(s.first)
        val two = s.joined(s.second)
        assertEquals("participant_joined", one.packet().type())
        f.service.leaveSession(s.session, s.second, f.storage.alice.id).fold({ throw it }, {})
        one.sendMessage(WebSocketMessage("cursor_update", cursor = CursorPosition(1, 2, 3)))
        two.policyClosed()
        one.sendMessage(WebSocketMessage("ping"))
        assertEquals("pong", one.packet().type())
        assertEquals(1, CollaborationWebSocketManager.getParticipantCount(s.session))
    }

    private suspend fun quotedJson(s: CollaborationSocketFixture) {
        val one = s.joined(s.first)
        val two = s.joined(s.second)
        assertEquals("participant_joined", one.packet().type())
        val text = "quotes \" and slash \\ and newline\nend"
        one.sendMessage(WebSocketMessage("operation", operation = OperationData("INSERT", 0, text, baseVersion = 1)))
        assertEquals("ack_operation", one.packet().type())
        val event = two.packet()
        assertEquals("operation", event.type())
        assertEquals(text, event.getValue("operation").jsonObject.getValue("text").jsonPrimitive.content)
    }

    private suspend fun unknownField(s: CollaborationSocketFixture) {
        val socket = s.joined(s.first)
        val before = f.snapshot()
        socket.send(Frame.Text("""{"type":"ping","futureExtension":{"supported":true}}"""))
        assertEquals("pong", socket.packet().type())
        assertEquals(before, f.snapshot())
    }

    private suspend fun malformed(s: CollaborationSocketFixture) {
        val socket = s.joined(s.first)
        val before = f.snapshot()
        socket.send(Frame.Text("{ invalid"))
        assertEquals(400, socket.packet().code())
        socket.sendMessage(WebSocketMessage("ping"))
        assertEquals("pong", socket.packet().type())
        assertEquals(before, f.snapshot())
    }
}
