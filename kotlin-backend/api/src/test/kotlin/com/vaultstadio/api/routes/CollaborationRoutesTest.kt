/**
 * VaultStadio Collaboration Routes Tests
 *
 * Unit tests for collaboration API data transfer objects.
 */

package com.vaultstadio.api.routes

import com.vaultstadio.api.routes.collaboration.AddReplyRequest
import com.vaultstadio.api.routes.collaboration.CollaborationSessionResponse
import com.vaultstadio.api.routes.collaboration.CollaborationWebSocketManager
import com.vaultstadio.api.routes.collaboration.CommentAnchorResponse
import com.vaultstadio.api.routes.collaboration.CommentReplyResponse
import com.vaultstadio.api.routes.collaboration.CommentResponse
import com.vaultstadio.api.routes.collaboration.CreateCommentRequest
import com.vaultstadio.api.routes.collaboration.CursorPositionResponse
import com.vaultstadio.api.routes.collaboration.DocumentStateResponse
import com.vaultstadio.api.routes.collaboration.JoinSessionRequest
import com.vaultstadio.api.routes.collaboration.OperationData
import com.vaultstadio.api.routes.collaboration.OperationRequest
import com.vaultstadio.api.routes.collaboration.ParticipantResponse
import com.vaultstadio.api.routes.collaboration.TextSelectionResponse
import com.vaultstadio.api.routes.collaboration.UpdateCursorRequest
import com.vaultstadio.api.routes.collaboration.UpdatePresenceRequest
import com.vaultstadio.api.routes.collaboration.UpdateSelectionRequest
import com.vaultstadio.api.routes.collaboration.UserPresenceResponse
import com.vaultstadio.api.routes.collaboration.WebSocketMessage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CollaborationRoutesTest {

    // ========================================================================
    // JoinSessionRequest Tests
    // ========================================================================

    @Test
    fun `JoinSessionRequest should store item ID`() {
        val request = JoinSessionRequest(itemId = "item-123")

        assertEquals("item-123", request.itemId)
    }

    @Test
    fun `JoinSessionRequest should accept different item IDs`() {
        val request1 = JoinSessionRequest(itemId = "folder-abc")
        val request2 = JoinSessionRequest(itemId = "doc-xyz-456")

        assertEquals("folder-abc", request1.itemId)
        assertEquals("doc-xyz-456", request2.itemId)
    }

    // ========================================================================
    // CollaborationSessionResponse Tests
    // ========================================================================

    @Test
    fun `CollaborationSessionResponse should store all properties`() {
        val response = CollaborationSessionResponse(
            id = "session-123",
            itemId = "item-456",
            participantId = "participant-789",
            participants = emptyList(),
            documentVersion = 5L,
            createdAt = "2024-01-15T10:30:00Z",
            expiresAt = "2024-01-15T12:30:00Z",
        )

        assertEquals("session-123", response.id)
        assertEquals("item-456", response.itemId)
        assertEquals("participant-789", response.participantId)
        assertTrue(response.participants.isEmpty())
        assertEquals(5L, response.documentVersion)
        assertEquals("2024-01-15T10:30:00Z", response.createdAt)
        assertEquals("2024-01-15T12:30:00Z", response.expiresAt)
    }

    @Test
    fun `CollaborationSessionResponse should store participants list`() {
        val participant = ParticipantResponse(
            id = "p-1",
            userId = "user-1",
            userName = "John",
            color = "#FF5733",
            cursor = null,
            selection = null,
            isEditing = false,
        )

        val response = CollaborationSessionResponse(
            id = "session-123",
            itemId = "item-456",
            participantId = "p-1",
            participants = listOf(participant),
            documentVersion = 0L,
            createdAt = "2024-01-15T10:30:00Z",
            expiresAt = "2024-01-15T12:30:00Z",
        )

        assertEquals(1, response.participants.size)
        assertEquals("John", response.participants[0].userName)
    }

    // ========================================================================
    // ParticipantResponse Tests
    // ========================================================================

    @Test
    fun `ParticipantResponse should store all properties`() {
        val cursor = CursorPositionResponse(line = 10, column = 5, offset = 150)
        val selection = TextSelectionResponse(
            start = CursorPositionResponse(line = 10, column = 5, offset = 150),
            end = CursorPositionResponse(line = 10, column = 20, offset = 165),
        )

        val response = ParticipantResponse(
            id = "participant-123",
            userId = "user-456",
            userName = "Alice",
            color = "#3498DB",
            cursor = cursor,
            selection = selection,
            isEditing = true,
        )

        assertEquals("participant-123", response.id)
        assertEquals("user-456", response.userId)
        assertEquals("Alice", response.userName)
        assertEquals("#3498DB", response.color)
        assertNotNull(response.cursor)
        assertNotNull(response.selection)
        assertTrue(response.isEditing)
    }

    @Test
    fun `ParticipantResponse should allow null cursor and selection`() {
        val response = ParticipantResponse(
            id = "participant-123",
            userId = "user-456",
            userName = "Bob",
            color = "#E74C3C",
            cursor = null,
            selection = null,
            isEditing = false,
        )

        assertNull(response.cursor)
        assertNull(response.selection)
        assertFalse(response.isEditing)
    }

    // ========================================================================
    // CursorPositionResponse Tests
    // ========================================================================

    @Test
    fun `CursorPositionResponse should store all properties`() {
        val cursor = CursorPositionResponse(
            line = 42,
            column = 15,
            offset = 1024,
        )

        assertEquals(42, cursor.line)
        assertEquals(15, cursor.column)
        assertEquals(1024, cursor.offset)
    }

    @Test
    fun `CursorPositionResponse should allow zero values`() {
        val cursor = CursorPositionResponse(line = 0, column = 0, offset = 0)

        assertEquals(0, cursor.line)
        assertEquals(0, cursor.column)
        assertEquals(0, cursor.offset)
    }

    // ========================================================================
    // TextSelectionResponse Tests
    // ========================================================================

    @Test
    fun `TextSelectionResponse should store start and end positions`() {
        val start = CursorPositionResponse(line = 5, column = 0, offset = 100)
        val end = CursorPositionResponse(line = 10, column = 50, offset = 250)

        val selection = TextSelectionResponse(start = start, end = end)

        assertEquals(5, selection.start.line)
        assertEquals(10, selection.end.line)
        assertEquals(100, selection.start.offset)
        assertEquals(250, selection.end.offset)
    }

    // ========================================================================
    // UpdateCursorRequest Tests
    // ========================================================================

    @Test
    fun `UpdateCursorRequest should store all properties`() {
        val request = UpdateCursorRequest(
            line = 25,
            column = 10,
            offset = 500,
        )

        assertEquals(25, request.line)
        assertEquals(10, request.column)
        assertEquals(500, request.offset)
    }

    // ========================================================================
    // UpdateSelectionRequest Tests
    // ========================================================================

    @Test
    fun `UpdateSelectionRequest should store start and end positions`() {
        val start = CursorPositionResponse(line = 1, column = 0, offset = 0)
        val end = CursorPositionResponse(line = 5, column = 30, offset = 150)

        val request = UpdateSelectionRequest(start = start, end = end)

        assertEquals(1, request.start.line)
        assertEquals(5, request.end.line)
    }

    // ========================================================================
    // OperationRequest Tests
    // ========================================================================

    @Test
    fun `OperationRequest INSERT should store text and position`() {
        val request = OperationRequest(
            type = "INSERT",
            position = 100,
            text = "Hello, World!",
            length = null,
            baseVersion = 5L,
        )

        assertEquals("INSERT", request.type)
        assertEquals(100, request.position)
        assertEquals("Hello, World!", request.text)
        assertNull(request.length)
        assertEquals(5L, request.baseVersion)
    }

    @Test
    fun `OperationRequest DELETE should store position and length`() {
        val request = OperationRequest(
            type = "DELETE",
            position = 50,
            text = null,
            length = 10,
            baseVersion = 3L,
        )

        assertEquals("DELETE", request.type)
        assertEquals(50, request.position)
        assertNull(request.text)
        assertEquals(10, request.length)
        assertEquals(3L, request.baseVersion)
    }

    @Test
    fun `OperationRequest RETAIN should store length`() {
        val request = OperationRequest(
            type = "RETAIN",
            position = 0,
            text = null,
            length = 100,
            baseVersion = 1L,
        )

        assertEquals("RETAIN", request.type)
        assertEquals(100, request.length)
    }

    // ========================================================================
    // DocumentStateResponse Tests
    // ========================================================================

    @Test
    fun `DocumentStateResponse should store all properties`() {
        val response = DocumentStateResponse(
            itemId = "item-123",
            version = 10L,
            content = "Hello, World!",
            lastModified = "2024-01-15T10:30:00Z",
        )

        assertEquals("item-123", response.itemId)
        assertEquals(10L, response.version)
        assertEquals("Hello, World!", response.content)
        assertEquals("2024-01-15T10:30:00Z", response.lastModified)
    }

    @Test
    fun `DocumentStateResponse should allow empty content`() {
        val response = DocumentStateResponse(
            itemId = "item-123",
            version = 0L,
            content = "",
            lastModified = "2024-01-15T10:30:00Z",
        )

        assertEquals("", response.content)
        assertEquals(0L, response.version)
    }

    // ========================================================================
    // CreateCommentRequest Tests
    // ========================================================================

    @Test
    fun `CreateCommentRequest should store all properties`() {
        val request = CreateCommentRequest(
            content = "This needs review",
            startLine = 10,
            startColumn = 5,
            endLine = 10,
            endColumn = 25,
            quotedText = "some selected text",
        )

        assertEquals("This needs review", request.content)
        assertEquals(10, request.startLine)
        assertEquals(5, request.startColumn)
        assertEquals(10, request.endLine)
        assertEquals(25, request.endColumn)
        assertEquals("some selected text", request.quotedText)
    }

    @Test
    fun `CreateCommentRequest should allow null quotedText`() {
        val request = CreateCommentRequest(
            content = "General comment",
            startLine = 1,
            startColumn = 0,
            endLine = 1,
            endColumn = 0,
            quotedText = null,
        )

        assertNull(request.quotedText)
    }

    // ========================================================================
    // CommentResponse Tests
    // ========================================================================

    @Test
    fun `CommentResponse should store all properties`() {
        val anchor = CommentAnchorResponse(
            startLine = 5,
            startColumn = 0,
            endLine = 5,
            endColumn = 50,
            quotedText = "quoted text",
        )

        val reply = CommentReplyResponse(
            id = "reply-1",
            userId = "user-2",
            userName = "Bob",
            content = "I agree",
            createdAt = "2024-01-15T11:00:00Z",
        )

        val response = CommentResponse(
            id = "comment-123",
            itemId = "item-456",
            userId = "user-1",
            userName = "Alice",
            content = "Please fix this",
            anchor = anchor,
            isResolved = false,
            replies = listOf(reply),
            createdAt = "2024-01-15T10:30:00Z",
            updatedAt = "2024-01-15T10:30:00Z",
        )

        assertEquals("comment-123", response.id)
        assertEquals("item-456", response.itemId)
        assertEquals("user-1", response.userId)
        assertEquals("Alice", response.userName)
        assertEquals("Please fix this", response.content)
        assertEquals(5, response.anchor.startLine)
        assertFalse(response.isResolved)
        assertEquals(1, response.replies.size)
    }

    @Test
    fun `CommentResponse should allow null userName`() {
        val anchor = CommentAnchorResponse(
            startLine = 1,
            startColumn = 0,
            endLine = 1,
            endColumn = 10,
            quotedText = null,
        )

        val response = CommentResponse(
            id = "comment-123",
            itemId = "item-456",
            userId = "user-1",
            userName = null,
            content = "Comment",
            anchor = anchor,
            isResolved = true,
            replies = emptyList(),
            createdAt = "2024-01-15T10:30:00Z",
            updatedAt = "2024-01-15T10:30:00Z",
        )

        assertNull(response.userName)
        assertTrue(response.isResolved)
    }

    // ========================================================================
    // CommentAnchorResponse Tests
    // ========================================================================

    @Test
    fun `CommentAnchorResponse should store all properties`() {
        val anchor = CommentAnchorResponse(
            startLine = 10,
            startColumn = 5,
            endLine = 15,
            endColumn = 20,
            quotedText = "selected code",
        )

        assertEquals(10, anchor.startLine)
        assertEquals(5, anchor.startColumn)
        assertEquals(15, anchor.endLine)
        assertEquals(20, anchor.endColumn)
        assertEquals("selected code", anchor.quotedText)
    }

    // ========================================================================
    // CommentReplyResponse Tests
    // ========================================================================

    @Test
    fun `CommentReplyResponse should store all properties`() {
        val reply = CommentReplyResponse(
            id = "reply-123",
            userId = "user-456",
            userName = "Charlie",
            content = "Good point!",
            createdAt = "2024-01-15T12:00:00Z",
        )

        assertEquals("reply-123", reply.id)
        assertEquals("user-456", reply.userId)
        assertEquals("Charlie", reply.userName)
        assertEquals("Good point!", reply.content)
        assertEquals("2024-01-15T12:00:00Z", reply.createdAt)
    }

    @Test
    fun `CommentReplyResponse should allow null userName`() {
        val reply = CommentReplyResponse(
            id = "reply-123",
            userId = "user-456",
            userName = null,
            content = "Reply content",
            createdAt = "2024-01-15T12:00:00Z",
        )

        assertNull(reply.userName)
    }

    // ========================================================================
    // AddReplyRequest Tests
    // ========================================================================

    @Test
    fun `AddReplyRequest should store content`() {
        val request = AddReplyRequest(content = "This is my reply")

        assertEquals("This is my reply", request.content)
    }

    @Test
    fun `AddReplyRequest should allow empty content`() {
        val request = AddReplyRequest(content = "")

        assertEquals("", request.content)
    }

    // ========================================================================
    // UpdatePresenceRequest Tests
    // ========================================================================

    @Test
    fun `UpdatePresenceRequest should store status and document`() {
        val request = UpdatePresenceRequest(
            status = "ONLINE",
            activeDocument = "doc-123",
        )

        assertEquals("ONLINE", request.status)
        assertEquals("doc-123", request.activeDocument)
    }

    @Test
    fun `UpdatePresenceRequest should support different statuses`() {
        val online = UpdatePresenceRequest(status = "ONLINE", activeDocument = null)
        val away = UpdatePresenceRequest(status = "AWAY", activeDocument = null)
        val busy = UpdatePresenceRequest(status = "BUSY", activeDocument = null)

        assertEquals("ONLINE", online.status)
        assertEquals("AWAY", away.status)
        assertEquals("BUSY", busy.status)
    }

    @Test
    fun `UpdatePresenceRequest should allow null activeDocument`() {
        val request = UpdatePresenceRequest(status = "ONLINE", activeDocument = null)

        assertNull(request.activeDocument)
    }

    // ========================================================================
    // UserPresenceResponse Tests
    // ========================================================================

    @Test
    fun `UserPresenceResponse should store all properties`() {
        val response = UserPresenceResponse(
            userId = "user-123",
            userName = "Alice",
            status = "ONLINE",
            lastSeen = "2024-01-15T10:30:00Z",
            activeDocument = "doc-456",
        )

        assertEquals("user-123", response.userId)
        assertEquals("Alice", response.userName)
        assertEquals("ONLINE", response.status)
        assertEquals("2024-01-15T10:30:00Z", response.lastSeen)
        assertEquals("doc-456", response.activeDocument)
    }

    @Test
    fun `UserPresenceResponse should allow null userName and activeDocument`() {
        val response = UserPresenceResponse(
            userId = "user-123",
            userName = null,
            status = "OFFLINE",
            lastSeen = "2024-01-15T10:30:00Z",
            activeDocument = null,
        )

        assertNull(response.userName)
        assertNull(response.activeDocument)
        assertEquals("OFFLINE", response.status)
    }

    // ========================================================================
    // WebSocketMessage Tests
    // ========================================================================

    @Test
    fun `WebSocketMessage should store type`() {
        val message = WebSocketMessage(type = "ping")

        assertEquals("ping", message.type)
    }

    @Test
    fun `WebSocketMessage should allow optional fields`() {
        val message = WebSocketMessage(
            type = "join",
            participantId = "p-123",
            itemId = null,
            cursor = null,
            selection = null,
            operation = null,
            presence = null,
        )

        assertEquals("join", message.type)
        assertEquals("p-123", message.participantId)
        assertNull(message.itemId)
    }

    // ========================================================================
    // OperationData Tests
    // ========================================================================

    @Test
    fun `OperationData INSERT should store text`() {
        val data = OperationData(
            type = "INSERT",
            position = 50,
            text = "new text",
            length = null,
            baseVersion = 10L,
        )

        assertEquals("INSERT", data.type)
        assertEquals(50, data.position)
        assertEquals("new text", data.text)
        assertNull(data.length)
    }

    @Test
    fun `OperationData DELETE should store length`() {
        val data = OperationData(
            type = "DELETE",
            position = 25,
            text = null,
            length = 15,
            baseVersion = 5L,
        )

        assertEquals("DELETE", data.type)
        assertEquals(25, data.position)
        assertNull(data.text)
        assertEquals(15, data.length)
    }

    // ========================================================================
    // CollaborationWebSocketManager Tests
    // ========================================================================

    @Test
    fun `CollaborationWebSocketManager should return 0 for non-existent session`() {
        val count = CollaborationWebSocketManager.getParticipantCount("non-existent-session")

        assertEquals(0, count)
    }
}
