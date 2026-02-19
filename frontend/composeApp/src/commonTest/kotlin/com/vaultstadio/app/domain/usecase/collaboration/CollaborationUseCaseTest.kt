/**
 * Unit tests for collaboration use cases (JoinCollaborationSession, GetCollaborationSession).
 * Uses a fake CollaborationRepository to avoid platform/DI.
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.CollaborationRepository
import com.vaultstadio.app.domain.model.CollaborationParticipant
import com.vaultstadio.app.domain.model.CollaborationSession
import com.vaultstadio.app.domain.model.CommentAnchor
import com.vaultstadio.app.domain.model.DocumentComment
import com.vaultstadio.app.domain.model.DocumentState
import com.vaultstadio.app.domain.model.PresenceStatus
import com.vaultstadio.app.domain.model.UserPresence
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testCollaborationSession(
    id: String = "session-1",
    itemId: String = "item-1",
    participantId: String = "part-1",
) = CollaborationSession(
    id = id,
    itemId = itemId,
    participantId = participantId,
    participants = emptyList(),
    documentVersion = 1L,
    createdAt = testInstant,
    expiresAt = testInstant,
)

private fun <T> stubResult(): Result<T> = Result.error("TEST", "Not implemented in fake")

private fun testDocumentComment(
    id: String = "comment-1",
    itemId: String = "item-1",
    content: String = "Review this",
) = DocumentComment(
    id = id,
    itemId = itemId,
    userId = "user-1",
    userName = "User",
    content = content,
    anchor = CommentAnchor(1, 0, 2, 0, null),
    isResolved = false,
    replies = emptyList(),
    createdAt = testInstant,
    updatedAt = testInstant,
)

private class FakeCollaborationRepository(
    var joinSessionResult: Result<CollaborationSession> = Result.success(testCollaborationSession()),
    var getSessionResult: Result<CollaborationSession> = Result.success(testCollaborationSession()),
    var leaveSessionResult: Result<Unit> = Result.success(Unit),
    var getCommentsResult: Result<List<DocumentComment>> = Result.success(emptyList()),
) : CollaborationRepository {

    override suspend fun joinSession(itemId: String): Result<CollaborationSession> = joinSessionResult

    override suspend fun leaveSession(sessionId: String): Result<Unit> = leaveSessionResult

    override suspend fun getSession(sessionId: String): Result<CollaborationSession> = getSessionResult

    override suspend fun getParticipants(sessionId: String): Result<List<CollaborationParticipant>> = stubResult()

    override suspend fun getDocumentState(itemId: String): Result<DocumentState> = stubResult()

    override suspend fun saveDocument(itemId: String): Result<Unit> = stubResult()

    override suspend fun getComments(itemId: String, includeResolved: Boolean): Result<List<DocumentComment>> =
        getCommentsResult

    override suspend fun createComment(
        itemId: String,
        content: String,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
    ): Result<Unit> = stubResult()

    override suspend fun resolveComment(itemId: String, commentId: String): Result<Unit> = stubResult()

    override suspend fun deleteComment(itemId: String, commentId: String): Result<Unit> = stubResult()

    override suspend fun updatePresence(status: PresenceStatus, activeDocument: String?): Result<Unit> = stubResult()

    override suspend fun getUserPresence(userIds: List<String>): Result<List<UserPresence>> = stubResult()

    override suspend fun setOffline(): Result<Unit> = stubResult()
}

class JoinCollaborationSessionUseCaseTest {

    @Test
    fun invoke_returnsRepositoryJoinSessionResult() = runTest {
        val session = testCollaborationSession(id = "s1", itemId = "doc-1")
        val repo = FakeCollaborationRepository(joinSessionResult = Result.success(session))
        val useCase = JoinCollaborationSessionUseCaseImpl(repo)
        val result = useCase("doc-1")
        assertTrue(result.isSuccess())
        assertEquals(session, result.getOrNull())
        assertEquals("doc-1", result.getOrNull()?.itemId)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeCollaborationRepository(joinSessionResult = Result.error("FORBIDDEN", "No access"))
        val useCase = JoinCollaborationSessionUseCaseImpl(repo)
        val result = useCase("item-1")
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class GetCollaborationSessionUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetSessionResult() = runTest {
        val session = testCollaborationSession(id = "s2")
        val repo = FakeCollaborationRepository(getSessionResult = Result.success(session))
        val useCase = GetCollaborationSessionUseCaseImpl(repo)
        val result = useCase("s2")
        assertTrue(result.isSuccess())
        assertEquals(session, result.getOrNull())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeCollaborationRepository(getSessionResult = Result.error("NOT_FOUND", "Session not found"))
        val useCase = GetCollaborationSessionUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
    }
}

class LeaveCollaborationSessionUseCaseTest {

    @Test
    fun invoke_returnsRepositoryLeaveSessionResult() = runTest {
        val repo = FakeCollaborationRepository(leaveSessionResult = Result.success(Unit))
        val useCase = LeaveCollaborationSessionUseCaseImpl(repo)
        val result = useCase("session-1")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeCollaborationRepository(leaveSessionResult = Result.error("NOT_FOUND", "Session not found"))
        val useCase = LeaveCollaborationSessionUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
    }
}

class GetDocumentCommentsUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetCommentsResult() = runTest {
        val comments = listOf(testDocumentComment("c1", "doc-1", "Please review"))
        val repo = FakeCollaborationRepository(getCommentsResult = Result.success(comments))
        val useCase = GetDocumentCommentsUseCaseImpl(repo)
        val result = useCase("doc-1", includeResolved = false)
        assertTrue(result.isSuccess())
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Please review", result.getOrNull()?.get(0)?.content)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeCollaborationRepository(getCommentsResult = Result.error("FORBIDDEN", "No access"))
        val useCase = GetDocumentCommentsUseCaseImpl(repo)
        val result = useCase("item-1")
        assertTrue(result.isError())
    }
}
