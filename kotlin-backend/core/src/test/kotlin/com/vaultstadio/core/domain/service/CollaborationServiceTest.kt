/**
 * VaultStadio Collaboration Service Tests
 */

package com.vaultstadio.core.domain.service

import arrow.core.right
import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.core.domain.model.CommentAnchor
import com.vaultstadio.core.domain.model.CursorPosition
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.core.domain.model.PresenceStatus
import com.vaultstadio.core.domain.model.TextSelection
import com.vaultstadio.core.domain.model.UserPresence
import com.vaultstadio.core.domain.repository.CollaborationRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class CollaborationServiceTest {

    private lateinit var collaborationRepository: CollaborationRepository
    private lateinit var service: CollaborationService

    @BeforeEach
    fun setup() {
        collaborationRepository = mockk()
        service = CollaborationService(collaborationRepository)
    }

    @Test
    fun `joinSession should create new session if none exists`() = runTest {
        val itemId = "item-1"
        val userId = "user-1"
        val userName = "John"

        coEvery { collaborationRepository.findActiveSessionForItem(itemId) } returns null.right()
        coEvery { collaborationRepository.createSession(any()) } answers {
            firstArg<CollaborationSession>().right()
        }
        coEvery { collaborationRepository.addParticipant(any(), any()) } answers {
            val session = CollaborationSession(
                id = "session-1",
                itemId = itemId,
                createdAt = Clock.System.now(),
                expiresAt = Clock.System.now() + 24.hours,
                participants = listOf(secondArg()),
            )
            session.right()
        }

        val result = service.joinSession(JoinSessionInput(itemId), userId, userName)

        assertTrue(result.isRight())
        result.onRight { (session, participant) ->
            assertEquals(itemId, session.itemId)
            assertEquals(userId, participant.userId)
            assertEquals(userName, participant.userName)
            assertTrue(participant.color.startsWith("#"))
        }
    }

    @Test
    fun `joinSession should join existing session`() = runTest {
        val itemId = "item-1"
        val userId = "user-2"
        val userName = "Jane"
        val now = Clock.System.now()

        val existingSession = CollaborationSession(
            id = "session-1",
            itemId = itemId,
            createdAt = now,
            expiresAt = now + 24.hours,
            participants = listOf(
                CollaborationParticipant(
                    id = "p1",
                    userId = "user-1",
                    userName = "John",
                    color = "#FF6B6B",
                    joinedAt = now,
                    lastActiveAt = now,
                ),
            ),
        )

        coEvery { collaborationRepository.findActiveSessionForItem(itemId) } returns existingSession.right()
        coEvery { collaborationRepository.addParticipant(any(), any()) } answers {
            val updated = existingSession.copy(
                participants = existingSession.participants + secondArg<CollaborationParticipant>(),
            )
            updated.right()
        }

        val result = service.joinSession(JoinSessionInput(itemId), userId, userName)

        assertTrue(result.isRight())
        result.onRight { (session, participant) ->
            assertEquals(2, session.participantCount)
            assertEquals(userName, participant.userName)
            // Should get different color than first participant
            assertTrue(participant.color != "#FF6B6B")
        }
    }

    @Test
    fun `updateCursor should update participant cursor`() = runTest {
        val sessionId = "session-1"
        val participantId = "p1"
        val now = Clock.System.now()

        val participant = CollaborationParticipant(
            id = participantId,
            userId = "user-1",
            userName = "John",
            color = "#FF6B6B",
            joinedAt = now,
            lastActiveAt = now,
        )

        coEvery { collaborationRepository.getParticipants(sessionId) } returns listOf(participant).right()
        coEvery { collaborationRepository.updateParticipant(sessionId, any()) } answers {
            secondArg<CollaborationParticipant>().right()
        }

        val cursor = CursorPosition(line = 10, column = 5, offset = 100)
        val result = service.updateCursor(sessionId, participantId, cursor)

        assertTrue(result.isRight())
        result.onRight { p ->
            assertEquals(cursor, p.cursor)
        }
    }

    @Test
    fun `updateSelection should update participant selection`() = runTest {
        val sessionId = "session-1"
        val participantId = "p1"
        val now = Clock.System.now()

        val participant = CollaborationParticipant(
            id = participantId,
            userId = "user-1",
            userName = "John",
            color = "#FF6B6B",
            joinedAt = now,
            lastActiveAt = now,
        )

        coEvery { collaborationRepository.getParticipants(sessionId) } returns listOf(participant).right()
        coEvery { collaborationRepository.updateParticipant(sessionId, any()) } answers {
            secondArg<CollaborationParticipant>().right()
        }

        val selection = TextSelection(
            start = CursorPosition(10, 0, 100),
            end = CursorPosition(10, 20, 120),
        )
        val result = service.updateSelection(sessionId, participantId, selection)

        assertTrue(result.isRight())
        result.onRight { p ->
            assertEquals(selection, p.selection)
            assertEquals(20, p.selection?.length)
        }
    }

    @Test
    fun `createComment should create new comment`() = runTest {
        val userId = "user-1"
        val input = CreateCommentInput(
            itemId = "item-1",
            content = "This needs review",
            startLine = 10,
            startColumn = 0,
            endLine = 15,
            endColumn = 20,
            quotedText = "some code",
        )

        coEvery { collaborationRepository.createComment(any()) } answers {
            firstArg<DocumentComment>().right()
        }

        val result = service.createComment(input, userId)

        assertTrue(result.isRight())
        result.onRight { comment ->
            assertEquals("item-1", comment.itemId)
            assertEquals(userId, comment.userId)
            assertEquals("This needs review", comment.content)
            assertEquals(10, comment.anchor.startLine)
            assertEquals("some code", comment.anchor.quotedText)
        }
    }

    @Test
    fun `resolveComment should mark comment as resolved`() = runTest {
        val commentId = "comment-1"
        val now = Clock.System.now()

        val comment = DocumentComment(
            id = commentId,
            itemId = "item-1",
            userId = "user-1",
            content = "Fixed",
            anchor = CommentAnchor(10, 0, 10, 50, null),
            resolvedAt = now,
            replies = emptyList(),
            createdAt = now,
            updatedAt = now,
        )

        coEvery { collaborationRepository.resolveComment(commentId, any()) } returns comment.right()

        val result = service.resolveComment(commentId)

        assertTrue(result.isRight())
        result.onRight { c ->
            assertTrue(c.isResolved)
        }
    }

    @Test
    fun `updatePresence should update user presence`() = runTest {
        val userId = "user-1"

        coEvery { collaborationRepository.updatePresence(any()) } answers {
            firstArg<UserPresence>().right()
        }

        val result = service.updatePresence(
            userId = userId,
            status = PresenceStatus.ONLINE,
            activeSession = "session-1",
            activeDocument = "doc-1",
        )

        assertTrue(result.isRight())
        result.onRight { presence ->
            assertEquals(userId, presence.userId)
            assertEquals(PresenceStatus.ONLINE, presence.status)
            assertEquals("session-1", presence.activeSession)
            assertEquals("doc-1", presence.activeDocument)
        }
    }
}
