/**
 * VaultStadio Collaboration WebSocket
 *
 * Manages WebSocket connection for real-time collaboration features.
 */

package com.vaultstadio.app.data.collaboration.websocket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WsCursorPosition(val line: Int, val column: Int)

@Serializable
data class CollaborationWsMessage(
    val type: String,
    val sessionId: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val content: String? = null,
    val cursorPosition: WsCursorPosition? = null,
    val version: Int? = null,
    val timestamp: Long? = null,
)

enum class CollaborationWsMessageType {
    JOIN,
    LEAVE,
    OPERATION,
    CURSOR_UPDATE,
    SYNC,
    ACK,
    ERROR,
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR,
}

class CollaborationWebSocket(
    private val baseUrl: String,
    private val itemId: String,
    private val sessionId: String,
    private val userId: String,
    private val userName: String,
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var client: HttpClient? = null
    private var connectionJob: Job? = null
    private val sendChannel = Channel<CollaborationWsMessage>(Channel.BUFFERED)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _messages = MutableSharedFlow<CollaborationWsMessage>(replay = 0)
    val messages: SharedFlow<CollaborationWsMessage> = _messages

    private val _errors = MutableSharedFlow<String>(replay = 0)
    val errors: SharedFlow<String> = _errors

    private val json = Json { ignoreUnknownKeys = true }

    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val baseReconnectDelay = 1000L

    fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTED ||
            _connectionState.value == ConnectionState.CONNECTING
        ) {
            return
        }
        connectionJob = scope.launch {
            connectInternal()
        }
    }

    private suspend fun connectInternal() {
        _connectionState.value = ConnectionState.CONNECTING
        try {
            client = HttpClient {
                install(WebSockets)
            }
            val wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://")
            val path = "/api/v1/collaboration/$itemId/ws?sessionId=$sessionId&userId=$userId"
            client?.webSocket(urlString = "$wsUrl$path") {
                _connectionState.value = ConnectionState.CONNECTED
                reconnectAttempts = 0
                val joinMessage = CollaborationWsMessage(
                    type = CollaborationWsMessageType.JOIN.name,
                    sessionId = sessionId,
                    userId = userId,
                    userName = userName,
                    timestamp = timeMillis(),
                )
                outgoing.send(Frame.Text(json.encodeToString(joinMessage)))
                val senderJob = launch {
                    for (message in sendChannel) {
                        if (isActive) {
                            outgoing.send(Frame.Text(json.encodeToString(message)))
                        }
                    }
                }
                try {
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                try {
                                    val message = json.decodeFromString<CollaborationWsMessage>(text)
                                    _messages.emit(message)
                                } catch (e: Exception) {
                                    _errors.emit("Failed to parse message: ${e.message}")
                                }
                            }
                            is Frame.Close -> break
                            else -> { }
                        }
                    }
                } finally {
                    senderJob.cancel()
                }
            }
        } catch (e: Exception) {
            _errors.emit("WebSocket connection failed: ${e.message}")
            _connectionState.value = ConnectionState.ERROR
            if (reconnectAttempts < maxReconnectAttempts) {
                reconnectAttempts++
                _connectionState.value = ConnectionState.RECONNECTING
                val delayMs = baseReconnectDelay * (1 shl minOf(reconnectAttempts - 1, 4))
                delay(delayMs)
                connectInternal()
            } else {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
        if (_connectionState.value == ConnectionState.CONNECTED) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun sendOperation(content: String, version: Int) {
        val message = CollaborationWsMessage(
            type = CollaborationWsMessageType.OPERATION.name,
            sessionId = sessionId,
            userId = userId,
            content = content,
            version = version,
            timestamp = timeMillis(),
        )
        scope.launch {
            sendChannel.send(message)
        }
    }

    fun sendCursorUpdate(line: Int, column: Int) {
        val message = CollaborationWsMessage(
            type = CollaborationWsMessageType.CURSOR_UPDATE.name,
            sessionId = sessionId,
            userId = userId,
            userName = userName,
            cursorPosition = WsCursorPosition(line, column),
            timestamp = timeMillis(),
        )
        scope.launch {
            sendChannel.send(message)
        }
    }

    suspend fun disconnect() {
        val leaveMessage = CollaborationWsMessage(
            type = CollaborationWsMessageType.LEAVE.name,
            sessionId = sessionId,
            userId = userId,
            userName = userName,
            timestamp = timeMillis(),
        )
        try {
            sendChannel.send(leaveMessage)
        } catch (e: Exception) { }
        connectionJob?.cancelAndJoin()
        client?.close()
        client = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun timeMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
}
