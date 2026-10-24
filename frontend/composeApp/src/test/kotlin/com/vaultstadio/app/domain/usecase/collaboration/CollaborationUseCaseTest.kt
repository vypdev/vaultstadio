/**
 * Unit tests for collaboration use cases (JoinCollaborationSession, GetCollaborationSession).
 * Uses a fake CollaborationRepository to avoid platform/DI.
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.data.collaboration.usecase.CreateDocumentCommentUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.DeleteDocumentCommentUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.GetCollaborationSessionUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.GetDocumentCommentsUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.GetDocumentStateUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.GetSessionParticipantsUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.GetUserPresenceUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.JoinCollaborationSessionUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.LeaveCollaborationSessionUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.ResolveDocumentCommentUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.SaveDocumentUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.SetOfflineUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.UpdatePresenceUseCaseImpl
import com.vaultstadio.app.domain.collaboration.CollaborationRepository
import com.vaultstadio.app.domain.collaboration.model.CollaborationParticipant
import com.vaultstadio.app.domain.collaboration.model.CollaborationSession
import com.vaultstadio.app.domain.collaboration.model.CommentAnchor
import com.vaultstadio.app.domain.collaboration.model.DocumentComment
import com.vaultstadio.app.domain.collaboration.model.DocumentState
import com.vaultstadio.app.domain.collaboration.model.PresenceStatus
import com.vaultstadio.app.domain.collaboration.model.UserPresence
import com.vaultstadio.app.domain.result.Result
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

private fun testDocumentState(itemId: String = "item-1") = DocumentState(
    itemId = itemId,
    version = 1L,
    content = "initial content",
    lastModified = testInstant,
)

private fun testParticipant(id: String = "p-1") = CollaborationParticipant(
    id = id,
    userId = "user-1",
    userName = "User",
    color = "#000000",
    cursor = null,
    selection = null,
    isEditing = false,
)

private fun testUserPresence() = UserPresence(
    userId = "user-1",
    userName = "User",
    status = PresenceStatus.ONLINE,
    lastSeen = testInstant,
    activeDocument = null,
)

private class FakeCollaborationRepository(
    var joinSessionResult: Result<CollaborationSession> = Result.success(testCollaborationSession()),
    var getSessionResult: Result<CollaborationSession> = Result.success(testCollaborationSession()),
    var leaveSessionResult: Result<Unit> = Result.success(Unit),
    var getCommentsResult: Result<List<DocumentComment>> = Result.success(emptyList()),
    var getParticipantsResult: Result<List<CollaborationParticipant>> = Result.success(emptyList()),
    var getDocumentStateResult: Result<DocumentState> = Result.success(testDocumentState()),
    var saveDocumentResult: Result<Unit> = Result.success(Unit),
    var createCommentResult: Result<Unit> = Result.success(Unit),
    var resolveCommentResult: Result<Unit> = Result.success(Unit),
    var deleteCommentResult: Result<Unit> = Result.success(Unit),
    var updatePresenceResult: Result<Unit> = Result.success(Unit),
    var getUserPresenceResult: Result<List<UserPresence>> = Result.success(emptyList()),
    var setOfflineResult: Result<Unit> = Result.success(Unit),
) : CollaborationRepository {

    override suspend fun joinSession(itemId: String): Result<CollaborationSession> = joinSessionResult

    override suspend fun leaveSession(sessionId: String): Result<Unit> = leaveSessionResult

    override suspend fun getSession(sessionId: String): Result<CollaborationSession> = getSessionResult

    override suspend fun getParticipants(sessionId: String): Result<List<CollaborationParticipant>> = getParticipantsResult

    override suspend fun getDocumentState(itemId: String): Result<DocumentState> = getDocumentStateResult

    override suspend fun saveDocument(itemId: String): Result<Unit> = saveDocumentResult

    override suspend fun getComments(itemId: String, includeResolved: Boolean): Result<List<DocumentComment>> =
        getCommentsResult

    override suspend fun createComment(
        itemId: String,
        content: String,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
    ): Result<Unit> = createCommentResult

    override suspend fun resolveComment(itemId: String, commentId: String): Result<Unit> = resolveCommentResult

    override suspend fun deleteComment(itemId: String, commentId: String): Result<Unit> = deleteCommentResult

    override suspend fun updatePresence(status: PresenceStatus, activeDocument: String?): Result<Unit> = updatePresenceResult

    override suspend fun getUserPresence(userIds: List<String>): Result<List<UserPresence>> = getUserPresenceResult

    override suspend fun setOffline(): Result<Unit> = setOfflineResult
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

class GetDocumentStateUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetDocumentStateResult() = runTest {
        val state = testDocumentState("doc-1")
        val repo = FakeCollaborationRepository(getDocumentStateResult = Result.success(state))
        val useCase = GetDocumentStateUseCaseImpl(repo)
        val result = useCase("doc-1")
        assertTrue(result.isSuccess())
        assertEquals(state, result.getOrNull())
        assertEquals("initial content", result.getOrNull()?.content)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeCollaborationRepository(getDocumentStateResult = Result.error("NOT_FOUND", "Document not found"))
        val useCase = GetDocumentStateUseCaseImpl(repo)
        val result = useCase("item-1")
        assertTrue(result.isError())
    }
}

class SaveDocumentUseCaseTest {

    @Test
    fun invoke_returnsRepositorySaveDocumentResult() = runTest {
        val repo = FakeCollaborationRepository(saveDocumentResult = Result.success(Unit))
        val useCase = SaveDocumentUseCaseImpl(repo)
        val result = useCase("doc-1")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeCollaborationRepository(saveDocumentResult = Result.error("CONFLICT", "Version conflict"))
        val useCase = SaveDocumentUseCaseImpl(repo)
        val result = useCase("item-1")
        assertTrue(result.isError())
    }
}

class GetSessionParticipantsUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetParticipantsResult() = runTest {
        val participants = listOf(testParticipant("p1"), testParticipant("p2"))
        val repo = FakeCollaborationRepository(getParticipantsResult = Result.success(participants))
        val useCase = GetSessionParticipantsUseCaseImpl(repo)
        val result = useCase("session-1")
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.size)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeCollaborationRepository(getParticipantsResult = Result.error("NOT_FOUND", "Session not found"))
        val useCase = GetSessionParticipantsUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
    }
}

class CreateDocumentCommentUseCaseTest {

    @Test
    fun invoke_returnsRepositoryCreateCommentResult() = runTest {
        val repo = FakeCollaborationRepository(createCommentResult = Result.success(Unit))
        val useCase = CreateDocumentCommentUseCaseImpl(repo)
        val result = useCase("doc-1", "Review this", 1, 0, 2, 0)
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeCollaborationRepository(createCommentResult = Result.error("FORBIDDEN", "No access"))
        val useCase = CreateDocumentCommentUseCaseImpl(repo)
        val result = useCase("doc-1", "Comment", 0, 0, 1, 0)
        assertTrue(result.isError())
    }
}

class ResolveDocumentCommentUseCaseTest {

    @Test
    fun invoke_returnsRepositoryResolveCommentResult() = runTest {
        val repo = FakeCollaborationRepository(resolveCommentResult = Result.success(Unit))
        val useCase = ResolveDocumentCommentUseCaseImpl(repo)
        val result = useCase("doc-1", "comment-1")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeCollaborationRepository(resolveCommentResult = Result.error("NOT_FOUND", "Comment not found"))
        val useCase = ResolveDocumentCommentUseCaseImpl(repo)
        val result = useCase("doc-1", "missing")
        assertTrue(result.isError())
    }
}

class DeleteDocumentCommentUseCaseTest {

    @Test
    fun invoke_returnsRepositoryDeleteCommentResult() = runTest {
        val repo = FakeCollaborationRepository(deleteCommentResult = Result.success(Unit))
        val useCase = DeleteDocumentCommentUseCaseImpl(repo)
        val result = useCase("doc-1", "comment-1")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeCollaborationRepository(deleteCommentResult = Result.error("FORBIDDEN", "Cannot delete"))
        val useCase = DeleteDocumentCommentUseCaseImpl(repo)
        val result = useCase("doc-1", "c1")
        assertTrue(result.isError())
    }
}

class UpdatePresenceUseCaseTest {

    @Test
    fun invoke_returnsRepositoryUpdatePresenceResult() = runTest {
        val repo = FakeCollaborationRepository(updatePresenceResult = Result.success(Unit))
        val useCase = UpdatePresenceUseCaseImpl(repo)
        val result = useCase(PresenceStatus.ONLINE, "item-1")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeCollaborationRepository(updatePresenceResult = Result.error("UNAVAILABLE", "Service down"))
        val useCase = UpdatePresenceUseCaseImpl(repo)
        val result = useCase(PresenceStatus.AWAY, null)
        assertTrue(result.isError())
    }
}

class GetUserPresenceUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetUserPresenceResult() = runTest {
        val presences = listOf(testUserPresence())
        val repo = FakeCollaborationRepository(getUserPresenceResult = Result.success(presences))
        val useCase = GetUserPresenceUseCaseImpl(repo)
        val result = useCase(listOf("user-1"))
        assertTrue(result.isSuccess())
        assertEquals(1, result.getOrNull()?.size)
        assertEquals(PresenceStatus.ONLINE, result.getOrNull()?.get(0)?.status)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeCollaborationRepository(getUserPresenceResult = Result.error("UNAUTHORIZED", "Not logged in"))
        val useCase = GetUserPresenceUseCaseImpl(repo)
        val result = useCase(listOf("user-1"))
        assertTrue(result.isError())
    }
}

class SetOfflineUseCaseTest {

    @Test
    fun invoke_returnsRepositorySetOfflineResult() = runTest {
        val repo = FakeCollaborationRepository(setOfflineResult = Result.success(Unit))
        val useCase = SetOfflineUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeCollaborationRepository(setOfflineResult = Result.error("NETWORK", "Failed to sync"))
        val useCase = SetOfflineUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
    }
}
