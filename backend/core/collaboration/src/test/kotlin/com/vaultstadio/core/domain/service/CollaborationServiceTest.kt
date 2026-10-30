package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.core.domain.model.CursorPosition
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.core.domain.model.PresenceStatus
import com.vaultstadio.core.domain.model.TextSelection
import com.vaultstadio.core.domain.model.UserPresence
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Original twelve regressions retained, now using explicit actors and valid canonical bindings. */
class CollaborationServiceTest {
    @Test
    fun `joinSession should create new session if none exists`() = runTest {
        val f = CollaborationServiceFixture()
        val result = f.service.joinSession(JoinSessionInput(f.item.id), "user-1", "John")
        val (session, participant) = assertIs<Either.Right<Pair<CollaborationSession, CollaborationParticipant>>>(
            result,
        ).value
        assertEquals(f.item.id, session.itemId)
        assertEquals("user-1", participant.userId)
        assertEquals("John", participant.userName)
        assertTrue(participant.color.startsWith("#"))
    }

    @Test
    fun `joinSession should join existing session`() = runTest {
        val f = CollaborationServiceFixture()
        f.item = f.item.copy(ownerId = "user-2")
        f.existing = f.session
        val result = f.service.joinSession(JoinSessionInput(f.item.id), "user-2", "Jane")
        val (session, participant) = assertIs<Either.Right<Pair<CollaborationSession, CollaborationParticipant>>>(
            result,
        ).value
        assertEquals(2, session.participantCount)
        assertEquals("Jane", participant.userName)
        assertTrue(participant.color != "#FF6B6B")
    }

    @Test
    fun `updateCursor should update participant cursor`() = runTest {
        val f = CollaborationServiceFixture()
        val cursor = CursorPosition(10, 5, 100)
        val result = f.service.updateCursor(f.session.id, "p1", cursor, "user-1")
        assertEquals(cursor, assertIs<Either.Right<CollaborationParticipant>>(result).value.cursor)
    }

    @Test
    fun `updateCursor should return Left when participant not found`() = runTest {
        val f = CollaborationServiceFixture()
        val result = f.service.updateCursor(f.session.id, "missing-p", CursorPosition(0, 0, 0), "user-1")
        assertIs<ItemNotFoundException>(assertIs<Either.Left<*>>(result).value)
    }

    @Test
    fun `updateSelection should update participant selection`() = runTest {
        val f = CollaborationServiceFixture()
        val selection = TextSelection(CursorPosition(10, 0, 100), CursorPosition(10, 20, 120))
        val result = f.service.updateSelection(f.session.id, "p1", selection, "user-1")
        val participant = assertIs<Either.Right<CollaborationParticipant>>(result).value
        assertEquals(selection, participant.selection)
        assertEquals(20, participant.selection?.length)
    }

    @Test
    fun `updateSelection should return Left when participant not found`() = runTest {
        val f = CollaborationServiceFixture()
        val result = f.service.updateSelection(f.session.id, "missing-p", null, "user-1")
        assertIs<ItemNotFoundException>(assertIs<Either.Left<*>>(result).value)
    }

    @Test
    fun `createComment should create new comment`() = runTest {
        val f = CollaborationServiceFixture()
        val input = CreateCommentInput(f.item.id, "This needs review", 10, 0, 15, 20, "some code")
        val result = f.service.createComment(input, "user-1")
        val comment = assertIs<Either.Right<DocumentComment>>(result).value
        assertEquals("item-1", comment.itemId)
        assertEquals("user-1", comment.userId)
        assertEquals("This needs review", comment.content)
        assertEquals(10, comment.anchor.startLine)
        assertEquals("some code", comment.anchor.quotedText)
    }

    @Test
    fun `resolveComment should mark comment as resolved`() = runTest {
        val f = CollaborationServiceFixture()
        val result = f.service.resolveComment(f.item.id, f.comment.id, "user-1")
        assertTrue(assertIs<Either.Right<DocumentComment>>(result).value.isResolved)
    }

    @Test
    fun `updatePresence should update user presence`() = runTest {
        val f = CollaborationServiceFixture()
        f.item = f.item.copy(id = "doc-1")
        f.session = f.session.copy(itemId = "doc-1")
        val result = f.service.updatePresence("user-1", PresenceStatus.ONLINE, "session-1", "doc-1")
        val presence = assertIs<Either.Right<UserPresence>>(result).value
        assertEquals("user-1", presence.userId)
        assertEquals(PresenceStatus.ONLINE, presence.status)
        assertEquals("session-1", presence.activeSession)
        assertEquals("doc-1", presence.activeDocument)
    }

    @Test
    fun `applyOperation should return Left when document state not found`() = runTest {
        val f = CollaborationServiceFixture()
        coEvery { f.repository.getDocumentState("item-1") } returns null.right()
        val result = f.service.applyOperation("item-1", f.operation, "user-1")
        assertIs<ItemNotFoundException>(assertIs<Either.Left<*>>(result).value)
    }

    @Test
    fun `deleteComment should return Left when comment not found`() = runTest {
        val f = CollaborationServiceFixture()
        coEvery { f.scopes.commentItem("comment-missing") } returns null.right()
        val result = f.service.deleteComment("item-1", "comment-missing", "user-1")
        assertIs<ItemNotFoundException>(assertIs<Either.Left<*>>(result).value)
    }

    @Test
    fun `deleteComment should return Left when user is not comment owner`() = runTest {
        val f = CollaborationServiceFixture()
        coEvery { f.repository.findComment(f.comment.id) } returns f.comment.copy(userId = "owner-1").right()
        val result = f.service.deleteComment("item-1", f.comment.id, "user-1")
        assertIs<AuthorizationException>(assertIs<Either.Left<*>>(result).value)
    }
}
