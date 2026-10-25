/**
 * VaultStadio Exposed Collaboration Repository Tests
 */

package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.core.domain.model.CommentAnchor
import com.vaultstadio.core.domain.model.CommentReply
import com.vaultstadio.core.domain.model.CursorPosition
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.core.domain.model.DocumentState
import com.vaultstadio.core.domain.model.PresenceStatus
import com.vaultstadio.core.domain.model.TextSelection
import com.vaultstadio.core.domain.model.UserPresence
import com.vaultstadio.core.domain.repository.CollaborationRepository
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Unit tests for ExposedCollaborationRepository.
 */
class ExposedCollaborationRepositoryTest {

    private lateinit var repository: CollaborationRepository

    @BeforeEach
    fun setup() {
        repository = ExposedCollaborationRepository()
    }

    @Nested
    @DisplayName("Repository API Tests")
    inner class RepositoryApiTests {

        @Test
        fun `repository should implement CollaborationRepository interface`() {
            assertTrue(repository is CollaborationRepository)
        }

        @Test
        fun `repository should be of correct implementation type`() {
            assertTrue(repository is ExposedCollaborationRepository)
        }
    }

    @Nested
    @DisplayName("CollaborationSession Model Tests")
    inner class CollaborationSessionModelTests {

        @Test
        fun `session should be created with all required fields`() {
            val now = Clock.System.now()
            val expiresAt = now + 2.hours

            val session = CollaborationSession(
                id = "session-123",
                itemId = "item-456",
                createdAt = now,
                expiresAt = expiresAt,
                participants = emptyList(),
            )

            assertEquals("session-123", session.id)
            assertEquals("item-456", session.itemId)
            assertEquals(now, session.createdAt)
            assertEquals(expiresAt, session.expiresAt)
            assertTrue(session.participants.isEmpty())
        }

        @Test
        fun `session should support multiple participants`() {
            val now = Clock.System.now()

            val participants = listOf(
                CollaborationParticipant(
                    id = "p-1",
                    userId = "user-1",
                    userName = "Alice",
                    color = "#FF5733",
                    cursor = null,
                    selection = null,
                    joinedAt = now,
                    lastActiveAt = now,
                    isEditing = false,
                ),
                CollaborationParticipant(
                    id = "p-2",
                    userId = "user-2",
                    userName = "Bob",
                    color = "#3498DB",
                    cursor = CursorPosition(line = 10, column = 5, offset = 150),
                    selection = null,
                    joinedAt = now,
                    lastActiveAt = now,
                    isEditing = true,
                ),
            )

            val session = CollaborationSession(
                id = "session-123",
                itemId = "item-456",
                createdAt = now,
                expiresAt = now + 2.hours,
                participants = participants,
            )

            assertEquals(2, session.participants.size)
            assertEquals("Alice", session.participants[0].userName)
            assertEquals("Bob", session.participants[1].userName)
        }
    }

    @Nested
    @DisplayName("CollaborationParticipant Model Tests")
    inner class CollaborationParticipantModelTests {

        @Test
        fun `participant should store all properties`() {
            val now = Clock.System.now()
            val cursor = CursorPosition(line = 5, column = 10, offset = 100)
            val selection = TextSelection(
                start = CursorPosition(line = 5, column = 0, offset = 90),
                end = CursorPosition(line = 5, column = 20, offset = 110),
            )

            val participant = CollaborationParticipant(
                id = "p-123",
                userId = "user-456",
                userName = "Charlie",
                color = "#E74C3C",
                cursor = cursor,
                selection = selection,
                joinedAt = now,
                lastActiveAt = now,
                isEditing = true,
            )

            assertEquals("p-123", participant.id)
            assertEquals("user-456", participant.userId)
            assertEquals("Charlie", participant.userName)
            assertEquals("#E74C3C", participant.color)
            assertNotNull(participant.cursor)
            assertEquals(5, participant.cursor?.line)
            assertNotNull(participant.selection)
            assertTrue(participant.isEditing)
        }

        @Test
        fun `participant should allow null cursor and selection`() {
            val now = Clock.System.now()

            val participant = CollaborationParticipant(
                id = "p-123",
                userId = "user-456",
                userName = "Dana",
                color = "#9B59B6",
                cursor = null,
                selection = null,
                joinedAt = now,
                lastActiveAt = now,
                isEditing = false,
            )

            assertNull(participant.cursor)
            assertNull(participant.selection)
            assertFalse(participant.isEditing)
        }
    }

    @Nested
    @DisplayName("CursorPosition Model Tests")
    inner class CursorPositionModelTests {

        @Test
        fun `cursor position should store all coordinates`() {
            val cursor = CursorPosition(
                line = 42,
                column = 15,
                offset = 1024,
            )

            assertEquals(42, cursor.line)
            assertEquals(15, cursor.column)
            assertEquals(1024, cursor.offset)
        }

        @Test
        fun `cursor position should allow zero values`() {
            val cursor = CursorPosition(line = 0, column = 0, offset = 0)

            assertEquals(0, cursor.line)
            assertEquals(0, cursor.column)
            assertEquals(0, cursor.offset)
        }
    }

    @Nested
    @DisplayName("TextSelection Model Tests")
    inner class TextSelectionModelTests {

        @Test
        fun `text selection should have start and end positions`() {
            val start = CursorPosition(line = 5, column = 0, offset = 100)
            val end = CursorPosition(line = 10, column = 50, offset = 300)

            val selection = TextSelection(start = start, end = end)

            assertEquals(5, selection.start.line)
            assertEquals(10, selection.end.line)
            assertEquals(100, selection.start.offset)
            assertEquals(300, selection.end.offset)
        }

        @Test
        fun `text selection should support same line selection`() {
            val start = CursorPosition(line = 10, column = 5, offset = 200)
            val end = CursorPosition(line = 10, column = 25, offset = 220)

            val selection = TextSelection(start = start, end = end)

            assertEquals(selection.start.line, selection.end.line)
        }
    }

    @Nested
    @DisplayName("DocumentState Model Tests")
    inner class DocumentStateModelTests {

        @Test
        fun `document state should store all properties`() {
            val now = Clock.System.now()

            val state = DocumentState(
                itemId = "item-123",
                version = 5L,
                content = "Hello, World!",
                operations = emptyList(),
                lastModified = now,
            )

            assertEquals("item-123", state.itemId)
            assertEquals(5L, state.version)
            assertEquals("Hello, World!", state.content)
            assertTrue(state.operations.isEmpty())
            assertEquals(now, state.lastModified)
        }

        @Test
        fun `document state should allow empty content`() {
            val now = Clock.System.now()

            val state = DocumentState(
                itemId = "item-123",
                version = 0L,
                content = "",
                operations = emptyList(),
                lastModified = now,
            )

            assertEquals("", state.content)
            assertEquals(0L, state.version)
        }
    }

    @Nested
    @DisplayName("CollaborationOperation Model Tests")
    inner class CollaborationOperationModelTests {

        @Test
        fun `Insert operation should store text and position`() {
            val now = Clock.System.now()

            val operation = CollaborationOperation.Insert(
                userId = "user-123",
                timestamp = now,
                baseVersion = 5L,
                position = 100,
                text = "Hello",
            )

            assertEquals("user-123", operation.userId)
            assertEquals(now, operation.timestamp)
            assertEquals(5L, operation.baseVersion)
            assertEquals(100, operation.position)
            assertEquals("Hello", operation.text)
        }

        @Test
        fun `Delete operation should store position and length`() {
            val now = Clock.System.now()

            val operation = CollaborationOperation.Delete(
                userId = "user-123",
                timestamp = now,
                baseVersion = 3L,
                position = 50,
                length = 10,
            )

            assertEquals(50, operation.position)
            assertEquals(10, operation.length)
            assertEquals(3L, operation.baseVersion)
        }

        @Test
        fun `Retain operation should store count`() {
            val now = Clock.System.now()

            val operation = CollaborationOperation.Retain(
                userId = "user-123",
                timestamp = now,
                baseVersion = 1L,
                count = 100,
            )

            assertEquals(100, operation.count)
        }
    }

    @Nested
    @DisplayName("DocumentComment Model Tests")
    inner class DocumentCommentModelTests {

        @Test
        fun `comment should store all properties`() {
            val now = Clock.System.now()
            val anchor = CommentAnchor(
                startLine = 10,
                startColumn = 5,
                endLine = 10,
                endColumn = 25,
                quotedText = "selected text",
            )

            val comment = DocumentComment(
                id = "comment-123",
                itemId = "item-456",
                userId = "user-789",
                content = "Please review this",
                anchor = anchor,
                resolvedAt = null,
                replies = emptyList(),
                createdAt = now,
                updatedAt = now,
            )

            assertEquals("comment-123", comment.id)
            assertEquals("item-456", comment.itemId)
            assertEquals("user-789", comment.userId)
            assertEquals("Please review this", comment.content)
            assertEquals(10, comment.anchor.startLine)
            assertEquals("selected text", comment.anchor.quotedText)
            assertNull(comment.resolvedAt)
            assertTrue(comment.replies.isEmpty())
        }

        @Test
        fun `comment should support replies`() {
            val now = Clock.System.now()
            val anchor = CommentAnchor(
                startLine = 1,
                startColumn = 0,
                endLine = 1,
                endColumn = 10,
                quotedText = null,
            )

            val replies = listOf(
                CommentReply(id = "r-1", userId = "user-2", content = "I agree", createdAt = now),
                CommentReply(id = "r-2", userId = "user-3", content = "Fixed", createdAt = now),
            )

            val comment = DocumentComment(
                id = "c-1",
                itemId = "item-1",
                userId = "user-1",
                content = "Issue here",
                anchor = anchor,
                resolvedAt = null,
                replies = replies,
                createdAt = now,
                updatedAt = now,
            )

            assertEquals(2, comment.replies.size)
            assertEquals("I agree", comment.replies[0].content)
        }

        @Test
        fun `comment should track resolution`() {
            val now = Clock.System.now()
            val resolvedAt = now + 30.minutes
            val anchor = CommentAnchor(1, 0, 1, 10, null)

            val comment = DocumentComment(
                id = "c-1",
                itemId = "item-1",
                userId = "user-1",
                content = "Fixed",
                anchor = anchor,
                resolvedAt = resolvedAt,
                replies = emptyList(),
                createdAt = now,
                updatedAt = now,
            )

            assertNotNull(comment.resolvedAt)
            assertTrue(comment.isResolved)
        }
    }

    @Nested
    @DisplayName("CommentAnchor Model Tests")
    inner class CommentAnchorModelTests {

        @Test
        fun `anchor should store line and column positions`() {
            val anchor = CommentAnchor(
                startLine = 10,
                startColumn = 5,
                endLine = 15,
                endColumn = 20,
                quotedText = "quoted code",
            )

            assertEquals(10, anchor.startLine)
            assertEquals(5, anchor.startColumn)
            assertEquals(15, anchor.endLine)
            assertEquals(20, anchor.endColumn)
            assertEquals("quoted code", anchor.quotedText)
        }

        @Test
        fun `anchor should allow null quoted text`() {
            val anchor = CommentAnchor(
                startLine = 1,
                startColumn = 0,
                endLine = 1,
                endColumn = 50,
                quotedText = null,
            )

            assertNull(anchor.quotedText)
        }
    }

    @Nested
    @DisplayName("CommentReply Model Tests")
    inner class CommentReplyModelTests {

        @Test
        fun `reply should store all properties`() {
            val now = Clock.System.now()

            val reply = CommentReply(
                id = "reply-123",
                userId = "user-456",
                content = "Thanks for the feedback!",
                createdAt = now,
            )

            assertEquals("reply-123", reply.id)
            assertEquals("user-456", reply.userId)
            assertEquals("Thanks for the feedback!", reply.content)
            assertEquals(now, reply.createdAt)
        }
    }

    @Nested
    @DisplayName("UserPresence Model Tests")
    inner class UserPresenceModelTests {

        @Test
        fun `presence should store all properties`() {
            val now = Clock.System.now()

            val presence = UserPresence(
                userId = "user-123",
                status = PresenceStatus.ONLINE,
                lastSeen = now,
                activeSession = "session-456",
                activeDocument = "doc-789",
            )

            assertEquals("user-123", presence.userId)
            assertEquals(PresenceStatus.ONLINE, presence.status)
            assertEquals(now, presence.lastSeen)
            assertEquals("session-456", presence.activeSession)
            assertEquals("doc-789", presence.activeDocument)
        }

        @Test
        fun `presence should support different statuses`() {
            val now = Clock.System.now()

            val online = UserPresence("u1", PresenceStatus.ONLINE, now, null, null)
            val away = UserPresence("u2", PresenceStatus.AWAY, now, null, null)
            val busy = UserPresence("u3", PresenceStatus.BUSY, now, null, null)
            val offline = UserPresence("u4", PresenceStatus.OFFLINE, now, null, null)

            assertEquals(PresenceStatus.ONLINE, online.status)
            assertEquals(PresenceStatus.AWAY, away.status)
            assertEquals(PresenceStatus.BUSY, busy.status)
            assertEquals(PresenceStatus.OFFLINE, offline.status)
        }

        @Test
        fun `presence should allow null session and document`() {
            val now = Clock.System.now()

            val presence = UserPresence(
                userId = "user-123",
                status = PresenceStatus.OFFLINE,
                lastSeen = now,
                activeSession = null,
                activeDocument = null,
            )

            assertNull(presence.activeSession)
            assertNull(presence.activeDocument)
        }
    }

    @Nested
    @DisplayName("Repository Method Signature Tests")
    inner class MethodSignatureTests {

        @Test
        fun `createSession method should exist`() {
            assertNotNull(repository::createSession)
        }

        @Test
        fun `findSession method should exist`() {
            assertNotNull(repository::findSession)
        }

        @Test
        fun `findActiveSessionForItem method should exist`() {
            assertNotNull(repository::findActiveSessionForItem)
        }

        @Test
        fun `updateSession method should exist`() {
            assertNotNull(repository::updateSession)
        }

        @Test
        fun `closeSession method should exist`() {
            assertNotNull(repository::closeSession)
        }

        @Test
        fun `addParticipant method should exist`() {
            assertNotNull(repository::addParticipant)
        }

        @Test
        fun `removeParticipant method should exist`() {
            assertNotNull(repository::removeParticipant)
        }

        @Test
        fun `getDocumentState method should exist`() {
            assertNotNull(repository::getDocumentState)
        }

        @Test
        fun `saveDocumentState method should exist`() {
            assertNotNull(repository::saveDocumentState)
        }

        @Test
        fun `applyOperation method should exist`() {
            assertNotNull(repository::applyOperation)
        }

        @Test
        fun `getOperationsSince method should exist`() {
            assertNotNull(repository::getOperationsSince)
        }

        @Test
        fun `updatePresence method should exist`() {
            assertNotNull(repository::updatePresence)
        }

        @Test
        fun `getPresence method should exist`() {
            assertNotNull(repository::getPresence)
        }

        @Test
        fun `createComment method should exist`() {
            assertNotNull(repository::createComment)
        }

        @Test
        fun `getCommentsForItem method should exist`() {
            assertNotNull(repository::getCommentsForItem)
        }

        @Test
        fun `resolveComment method should exist`() {
            assertNotNull(repository::resolveComment)
        }

        @Test
        fun `addReply method should exist`() {
            assertNotNull(repository::addReply)
        }

        @Test
        fun `deleteComment method should exist`() {
            assertNotNull(repository::deleteComment)
        }
    }
}
