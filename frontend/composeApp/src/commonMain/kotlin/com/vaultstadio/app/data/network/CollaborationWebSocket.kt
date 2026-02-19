/**
 * VaultStadio Collaboration WebSocket
 *
 * Manages WebSocket connection for real-time collaboration features.
 * Supports:
 * - Session join/leave notifications
 * - Document operations (text changes)
 * - Cursor position updates
 * - Auto-reconnection on disconnect
 */

package com.vaultstadio.app.data.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
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

/**
 * WebSocket message types for collaboration.
 */
enum class CollaborationMessageType {
    JOIN,
    LEAVE,
    OPERATION,
    CURSOR_UPDATE,
    SYNC,
    ACK,
    ERROR,
}

/**
 * WebSocket message for collaboration.
 */
@Serializable
data class CollaborationMessage(
    val type: String,
    val sessionId: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val content: String? = null,
    val cursorPosition: CursorPosition? = null,
    val version: Int? = null,
    val timestamp: Long? = null,
)

/**
 * Cursor position data.
 */
@Serializable
data class CursorPosition(
    val line: Int,
    val column: Int,
)

/**
 * Connection state for WebSocket.
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR,
}

/**
 * Manages a WebSocket connection for real-time collaboration.
 */
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
    private val sendChannel = Channel<CollaborationMessage>(Channel.BUFFERED)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _messages = MutableSharedFlow<CollaborationMessage>(replay = 0)
    val messages: SharedFlow<CollaborationMessage> = _messages

    private val _errors = MutableSharedFlow<String>(replay = 0)
    val errors: SharedFlow<String> = _errors

    private val json = Json { ignoreUnknownKeys = true }

    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val baseReconnectDelay = 1000L

    /**
     * Connects to the collaboration WebSocket.
     */
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

                // Send join message
                val joinMessage = CollaborationMessage(
                    type = CollaborationMessageType.JOIN.name,
                    sessionId = sessionId,
                    userId = userId,
                    userName = userName,
                    timestamp = System.currentTimeMillis(),
                )
                outgoing.send(Frame.Text(json.encodeToString(joinMessage)))

                // Launch sender coroutine
                val senderJob = launch {
                    for (message in sendChannel) {
                        if (isActive) {
                            outgoing.send(Frame.Text(json.encodeToString(message)))
                        }
                    }
                }

                // Receive messages
                try {
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                try {
                                    val message = json.decodeFromString<CollaborationMessage>(text)
                                    _messages.emit(message)
                                } catch (e: Exception) {
                                    _errors.emit("Failed to parse message: ${e.message}")
                                }
                            }
                            is Frame.Close -> {
                                break
                            }
                            else -> { /* Ignore other frame types */ }
                        }
                    }
                } finally {
                    senderJob.cancel()
                }
            }
        } catch (e: Exception) {
            _errors.emit("WebSocket connection failed: ${e.message}")
            _connectionState.value = ConnectionState.ERROR

            // Attempt reconnection
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

        // Connection closed
        if (_connectionState.value == ConnectionState.CONNECTED) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    /**
     * Sends a document operation (content change).
     */
    fun sendOperation(content: String, version: Int) {
        val message = CollaborationMessage(
            type = CollaborationMessageType.OPERATION.name,
            sessionId = sessionId,
            userId = userId,
            content = content,
            version = version,
            timestamp = System.currentTimeMillis(),
        )
        scope.launch {
            sendChannel.send(message)
        }
    }

    /**
     * Sends a cursor position update.
     */
    fun sendCursorUpdate(line: Int, column: Int) {
        val message = CollaborationMessage(
            type = CollaborationMessageType.CURSOR_UPDATE.name,
            sessionId = sessionId,
            userId = userId,
            userName = userName,
            cursorPosition = CursorPosition(line, column),
            timestamp = System.currentTimeMillis(),
        )
        scope.launch {
            sendChannel.send(message)
        }
    }

    /**
     * Disconnects from the WebSocket.
     */
    suspend fun disconnect() {
        // Send leave message
        val leaveMessage = CollaborationMessage(
            type = CollaborationMessageType.LEAVE.name,
            sessionId = sessionId,
            userId = userId,
            userName = userName,
            timestamp = System.currentTimeMillis(),
        )
        try {
            sendChannel.send(leaveMessage)
        } catch (e: Exception) {
            // Ignore send errors on disconnect
        }

        connectionJob?.cancelAndJoin()
        client?.close()
        client = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    companion object {
        // Use System.currentTimeMillis() for cross-platform compatibility
        // In Kotlin/Native this may need platform-specific implementation
        private fun System.currentTimeMillis(): Long {
            return kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        }
    }

    // Helper object for time
    private object System {
        fun currentTimeMillis(): Long {
            return kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        }
    }
}
