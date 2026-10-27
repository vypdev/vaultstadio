package com.vaultstadio.api.routes.collaboration

import com.vaultstadio.core.domain.service.JoinSessionInput
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.bearerAuth
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

internal class CollaborationSocketFixture(val application: CollaborationIntegrationFixture, val client: HttpClient) {
    lateinit var session: String
    lateinit var first: String
    lateinit var second: String
    val peers = mutableListOf<DefaultClientWebSocketSession>()

    suspend fun prepare() {
        val f = application
        val actor = f.storage.alice.id
        val one = f.service.joinSession(JoinSessionInput(f.item.id), actor, "First device").fold({ throw it }, { it })
        val two = f.service.joinSession(JoinSessionInput(f.item.id), actor, "Second device").fold({ throw it }, { it })
        session = one.first.id
        first = one.second.id
        second = two.second.id
    }

    suspend fun open(actor: String = "owner"): DefaultClientWebSocketSession = client.webSocketSession(
        "/api/v1/collaboration/ws/$session",
    ) {
        bearerAuth(actor)
    }.also { peers.add(it) }

    suspend fun joined(participant: String): DefaultClientWebSocketSession = open().also {
        it.sendMessage(WebSocketMessage("join", participantId = participant))
        assertEquals("joined", it.packet().type())
    }

    suspend fun close() {
        for (peer in peers) peer.close()
    }
}

internal suspend fun DefaultClientWebSocketSession.sendMessage(message: WebSocketMessage) =
    send(Frame.Text(Json.encodeToString(message)))

internal suspend fun DefaultClientWebSocketSession.packet(): JsonObject = withTimeout(5_000) {
    val frame = assertIs<Frame.Text>(incoming.receive())
    Json.parseToJsonElement(frame.readText()).jsonObject
}

internal fun JsonObject.type(): String = getValue("type").jsonPrimitive.content
internal fun JsonObject.code(): Int = getValue("code").jsonPrimitive.content.toInt()

internal suspend fun DefaultClientWebSocketSession.policyClosed() = withTimeout(5_000) {
    assertFalse(incoming.receiveCatching().getOrNull() is Frame.Text)
    assertEquals(1008, closeReason.await()?.code?.toInt())
}
