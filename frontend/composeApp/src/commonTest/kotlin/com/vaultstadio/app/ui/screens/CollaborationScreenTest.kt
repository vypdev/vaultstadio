/**
 * VaultStadio Collaboration Screen Tests
 */

package com.vaultstadio.app.ui.screens

import com.vaultstadio.app.domain.collaboration.model.CollaborationParticipant
import com.vaultstadio.app.domain.collaboration.model.CollaborationSession
import com.vaultstadio.app.domain.collaboration.model.CommentAnchor
import com.vaultstadio.app.domain.collaboration.model.DocumentComment
import com.vaultstadio.app.domain.collaboration.model.DocumentState
import com.vaultstadio.app.domain.collaboration.model.PresenceStatus
import com.vaultstadio.app.domain.collaboration.model.UserPresence
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollaborationScreenTest {

    @Test
    fun testCollaborationSessionModel() {
        val now = Clock.System.now()
        val participants = listOf(
            CollaborationParticipant(
                id = "p1",
                userId = "user1",
                userName = "Alice",
                color = "#FF0000",
                isEditing = true,
            ),
        )

        val session = CollaborationSession(
            id = "s1",
            itemId = "item1",
            participantId = "p1",
            participants = participants,
            documentVersion = 5,
            createdAt = now,
            expiresAt = now,
        )

        assertEquals("s1", session.id)
        assertEquals(1, session.participants.size)
        assertEquals(5L, session.documentVersion)
    }

    @Test
    fun testCollaborationParticipantModel() {
        val participant = CollaborationParticipant(
            id = "p1",
            userId = "user1",
            userName = "Alice",
            color = "#3498DB",
            isEditing = true,
        )

        assertEquals("Alice", participant.userName)
        assertEquals("#3498DB", participant.color)
        assertTrue(participant.isEditing)
    }

    @Test
    fun testDocumentStateModel() {
        val now = Clock.System.now()
        val state = DocumentState(
            itemId = "item1",
            version = 10,
            content = "Hello World",
            lastModified = now,
        )

        assertEquals("Hello World", state.content)
        assertEquals(10L, state.version)
    }

    @Test
    fun testDocumentCommentModel() {
        val now = Clock.System.now()
        val anchor = CommentAnchor(
            startLine = 1,
            startColumn = 0,
            endLine = 1,
            endColumn = 10,
            quotedText = "Hello",
        )

        val comment = DocumentComment(
            id = "c1",
            itemId = "item1",
            userId = "user1",
            userName = "Alice",
            content = "Nice work!",
            anchor = anchor,
            replies = emptyList(),
            isResolved = false,
            createdAt = now,
            updatedAt = now,
        )

        assertEquals("Nice work!", comment.content)
        assertFalse(comment.isResolved)
        assertEquals("Hello", comment.anchor.quotedText)
    }

    @Test
    fun testDocumentCommentResolved() {
        val now = Clock.System.now()
        val anchor = CommentAnchor(1, 0, 1, 10, null)

        val comment = DocumentComment(
            id = "c2",
            itemId = "item1",
            userId = "user1",
            userName = "Bob",
            content = "Fixed!",
            anchor = anchor,
            replies = emptyList(),
            isResolved = true,
            createdAt = now,
            updatedAt = now,
        )

        assertTrue(comment.isResolved)
    }

    @Test
    fun testPresenceStatusEnumValues() {
        val statuses = PresenceStatus.entries
        assertEquals(4, statuses.size)
        assertTrue(statuses.contains(PresenceStatus.ONLINE))
        assertTrue(statuses.contains(PresenceStatus.AWAY))
        assertTrue(statuses.contains(PresenceStatus.BUSY))
        assertTrue(statuses.contains(PresenceStatus.OFFLINE))
    }

    @Test
    fun testUserPresenceModel() {
        val now = Clock.System.now()
        val presence = UserPresence(
            userId = "user1",
            status = PresenceStatus.ONLINE,
            activeDocument = "doc1",
            lastSeen = now,
        )

        assertEquals(PresenceStatus.ONLINE, presence.status)
        assertEquals("doc1", presence.activeDocument)
    }
}
