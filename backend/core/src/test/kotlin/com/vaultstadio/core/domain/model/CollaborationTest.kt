/**
 * VaultStadio Collaboration Model Tests
 */

package com.vaultstadio.core.domain.model

import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class CollaborationTest {

    @Test
    fun `CollaborationSession should track participants`() {
        val now = Clock.System.now()
        val session = CollaborationSession(
            id = "session-1",
            itemId = "item-1",
            createdAt = now,
            expiresAt = now + 24.hours,
            participants = listOf(
                CollaborationParticipant(
                    id = "participant-1",
                    userId = "user-1",
                    userName = "John",
                    color = "#FF6B6B",
                    joinedAt = now,
                    lastActiveAt = now,
                ),
            ),
        )

        assertEquals("session-1", session.id)
        assertEquals("item-1", session.itemId)
        assertEquals(1, session.participantCount)
        assertTrue(session.isActive)
    }

    @Test
    fun `CollaborationSession should be inactive with no participants`() {
        val now = Clock.System.now()
        val session = CollaborationSession(
            id = "session-1",
            itemId = "item-1",
            createdAt = now,
            expiresAt = now + 24.hours,
            participants = emptyList(),
        )

        assertEquals(0, session.participantCount)
        assertFalse(session.isActive)
    }

    @Test
    fun `CollaborationParticipant should have cursor and selection`() {
        val now = Clock.System.now()
        val cursor = CursorPosition(line = 10, column = 5, offset = 100)
        val selection = TextSelection(
            start = CursorPosition(10, 5, 100),
            end = CursorPosition(10, 20, 115),
        )

        val participant = CollaborationParticipant(
            id = "participant-1",
            userId = "user-1",
            userName = "John",
            color = "#4ECDC4",
            cursor = cursor,
            selection = selection,
            joinedAt = now,
            lastActiveAt = now,
            isEditing = true,
        )

        assertNotNull(participant.cursor)
        assertNotNull(participant.selection)
        assertEquals(10, participant.cursor!!.line)
        assertEquals(15, participant.selection!!.length)
        assertTrue(participant.isEditing)
    }

    @Test
    fun `TextSelection should calculate length correctly`() {
        val selection = TextSelection(
            start = CursorPosition(0, 0, 0),
            end = CursorPosition(0, 10, 10),
        )

        assertEquals(10, selection.length)
        assertFalse(selection.isEmpty)
    }

    @Test
    fun `TextSelection should be empty when start equals end`() {
        val position = CursorPosition(5, 10, 50)
        val selection = TextSelection(start = position, end = position)

        assertEquals(0, selection.length)
        assertTrue(selection.isEmpty)
    }

    @Test
    fun `CollaborationOperation Insert should have correct properties`() {
        val now = Clock.System.now()
        val operation = CollaborationOperation.Insert(
            userId = "user-1",
            timestamp = now,
            baseVersion = 5,
            position = 100,
            text = "Hello World",
        )

        assertEquals("user-1", operation.userId)
        assertEquals(5, operation.baseVersion)
        assertEquals(100, operation.position)
        assertEquals("Hello World", operation.text)
    }

    @Test
    fun `CollaborationOperation Delete should have correct properties`() {
        val now = Clock.System.now()
        val operation = CollaborationOperation.Delete(
            userId = "user-1",
            timestamp = now,
            baseVersion = 5,
            position = 50,
            length = 10,
            deletedText = "some text",
        )

        assertEquals(50, operation.position)
        assertEquals(10, operation.length)
        assertEquals("some text", operation.deletedText)
    }

    @Test
    fun `DocumentState should track version and content`() {
        val now = Clock.System.now()
        val state = DocumentState(
            itemId = "item-1",
            version = 10,
            content = "Document content here",
            operations = emptyList(),
            lastModified = now,
        )

        assertEquals("item-1", state.itemId)
        assertEquals(10, state.version)
        assertEquals("Document content here", state.content)
    }

    @Test
    fun `PresenceStatus should have all status values`() {
        val statuses = PresenceStatus.entries

        assertTrue(statuses.contains(PresenceStatus.ONLINE))
        assertTrue(statuses.contains(PresenceStatus.AWAY))
        assertTrue(statuses.contains(PresenceStatus.BUSY))
        assertTrue(statuses.contains(PresenceStatus.OFFLINE))
    }

    @Test
    fun `DocumentComment should track resolution state`() {
        val now = Clock.System.now()
        val comment = DocumentComment(
            id = "comment-1",
            itemId = "item-1",
            userId = "user-1",
            content = "This needs review",
            anchor = CommentAnchor(
                startLine = 10,
                startColumn = 0,
                endLine = 15,
                endColumn = 20,
                quotedText = "code snippet",
            ),
            resolvedAt = null,
            replies = emptyList(),
            createdAt = now,
            updatedAt = now,
        )

        assertFalse(comment.isResolved)
        assertEquals("This needs review", comment.content)
        assertEquals("code snippet", comment.anchor.quotedText)
    }

    @Test
    fun `DocumentComment should be resolved when resolvedAt is set`() {
        val now = Clock.System.now()
        val comment = DocumentComment(
            id = "comment-1",
            itemId = "item-1",
            userId = "user-1",
            content = "Fixed",
            anchor = CommentAnchor(10, 0, 10, 50, null),
            resolvedAt = now,
            replies = emptyList(),
            createdAt = now,
            updatedAt = now,
        )

        assertTrue(comment.isResolved)
    }
}
