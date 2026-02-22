/**
 * Unit tests for CollaborationViewModel: loadSession, loadParticipants, leaveSession,
 * updatePresence, loadUserPresences, setOffline, clearError.
 * Uses initialItemId = "" so init does not trigger joinSession/WebSocket.
 */

package com.vaultstadio.app.feature.collaboration

import com.vaultstadio.app.domain.auth.model.User
import com.vaultstadio.app.domain.auth.model.UserRole
import com.vaultstadio.app.domain.auth.usecase.GetCurrentUserUseCase
import com.vaultstadio.app.domain.collaboration.model.CollaborationParticipant
import com.vaultstadio.app.domain.collaboration.model.CollaborationSession
import com.vaultstadio.app.domain.collaboration.model.DocumentComment
import com.vaultstadio.app.domain.collaboration.model.DocumentState
import com.vaultstadio.app.domain.collaboration.model.PresenceStatus
import com.vaultstadio.app.domain.collaboration.model.UserPresence
import com.vaultstadio.app.domain.collaboration.usecase.CreateDocumentCommentUseCase
import com.vaultstadio.app.domain.collaboration.usecase.DeleteDocumentCommentUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetCollaborationSessionUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetDocumentCommentsUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetDocumentStateUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetSessionParticipantsUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetUserPresenceUseCase
import com.vaultstadio.app.domain.collaboration.usecase.JoinCollaborationSessionUseCase
import com.vaultstadio.app.domain.collaboration.usecase.LeaveCollaborationSessionUseCase
import com.vaultstadio.app.domain.collaboration.usecase.ResolveDocumentCommentUseCase
import com.vaultstadio.app.domain.collaboration.usecase.SaveDocumentUseCase
import com.vaultstadio.app.domain.collaboration.usecase.SetOfflineUseCase
import com.vaultstadio.app.domain.collaboration.usecase.UpdatePresenceUseCase
import com.vaultstadio.app.domain.config.usecase.GetCollaborationUrlUseCase
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.feature.ViewModelTestBase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testUser() = User(
    id = "user-1",
    email = "u@test.com",
    username = "TestUser",
    role = UserRole.USER,
    avatarUrl = null,
    createdAt = testInstant,
)

private fun testSession(
    id: String = "sess-1",
    itemId: String = "item-1",
) = CollaborationSession(
    id = id,
    itemId = itemId,
    participantId = "p-1",
    participants = emptyList(),
    documentVersion = 0L,
    createdAt = testInstant,
    expiresAt = testInstant,
)

private fun testParticipant() = CollaborationParticipant(
    id = "p-1",
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

private class FakeJoinCollaborationSessionUseCase(
    var result: Result<CollaborationSession> = Result.success(testSession()),
) : JoinCollaborationSessionUseCase {
    override suspend fun invoke(itemId: String): Result<CollaborationSession> = result
}

private class FakeLeaveCollaborationSessionUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : LeaveCollaborationSessionUseCase {
    override suspend fun invoke(sessionId: String): Result<Unit> = result
}

private class FakeGetCollaborationSessionUseCase(
    var result: Result<CollaborationSession> = Result.success(testSession()),
) : GetCollaborationSessionUseCase {
    override suspend fun invoke(sessionId: String): Result<CollaborationSession> = result
}

private class FakeGetSessionParticipantsUseCase(
    var result: Result<List<CollaborationParticipant>> = Result.success(emptyList()),
) : GetSessionParticipantsUseCase {
    override suspend fun invoke(sessionId: String): Result<List<CollaborationParticipant>> = result
}

private class FakeGetDocumentStateUseCase : GetDocumentStateUseCase {
    override suspend fun invoke(itemId: String): Result<DocumentState> =
        Result.success(
            DocumentState(
                itemId = itemId,
                version = 0L,
                content = "",
                lastModified = testInstant,
            ),
        )
}

private class FakeSaveDocumentUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : SaveDocumentUseCase {
    override suspend fun invoke(itemId: String): Result<Unit> = result
}

private class FakeGetDocumentCommentsUseCase : GetDocumentCommentsUseCase {
    override suspend fun invoke(itemId: String, includeResolved: Boolean): Result<List<DocumentComment>> =
        Result.success(emptyList())
}

private class FakeCreateDocumentCommentUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : CreateDocumentCommentUseCase {
    override suspend fun invoke(
        itemId: String,
        content: String,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
    ): Result<Unit> = result
}

private class FakeResolveDocumentCommentUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : ResolveDocumentCommentUseCase {
    override suspend fun invoke(itemId: String, commentId: String): Result<Unit> = result
}

private class FakeDeleteDocumentCommentUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : DeleteDocumentCommentUseCase {
    override suspend fun invoke(itemId: String, commentId: String): Result<Unit> = result
}

private class FakeUpdatePresenceUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : UpdatePresenceUseCase {
    override suspend fun invoke(status: PresenceStatus, activeDocument: String?): Result<Unit> = result
}

private class FakeGetUserPresenceUseCase(
    var result: Result<List<UserPresence>> = Result.success(emptyList()),
) : GetUserPresenceUseCase {
    override suspend fun invoke(userIds: List<String>): Result<List<UserPresence>> = result
}

private class FakeSetOfflineUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : SetOfflineUseCase {
    override suspend fun invoke(): Result<Unit> = result
}

private class FakeGetCollaborationUrlUseCase(
    private val url: String = "ws://test/collab",
) : GetCollaborationUrlUseCase {
    override fun invoke(): String = url
}

private class FakeGetCurrentUserUseCase(
    private val user: User? = testUser(),
) : GetCurrentUserUseCase {
    override val currentUserFlow = MutableStateFlow(user)
    override suspend fun invoke(): Result<User> =
        user?.let { Result.success(it) } ?: Result.error("", "No user")
    override suspend fun refresh() {}
    override fun isLoggedIn(): Boolean = user != null
}

class CollaborationViewModelTest {

    private fun createViewModel(
        initialItemId: String = "",
        getSessionResult: Result<CollaborationSession> = Result.success(testSession()),
        getParticipantsResult: Result<List<CollaborationParticipant>> = Result.success(emptyList()),
        leaveSessionResult: Result<Unit> = Result.success(Unit),
        updatePresenceResult: Result<Unit> = Result.success(Unit),
        getUserPresenceResult: Result<List<UserPresence>> = Result.success(emptyList()),
        setOfflineResult: Result<Unit> = Result.success(Unit),
    ): CollaborationViewModel = CollaborationViewModel(
        joinSessionUseCase = FakeJoinCollaborationSessionUseCase(),
        leaveSessionUseCase = FakeLeaveCollaborationSessionUseCase(leaveSessionResult),
        getSessionUseCase = FakeGetCollaborationSessionUseCase(getSessionResult),
        getParticipantsUseCase = FakeGetSessionParticipantsUseCase(getParticipantsResult),
        getDocumentStateUseCase = FakeGetDocumentStateUseCase(),
        saveDocumentUseCase = FakeSaveDocumentUseCase(),
        getCommentsUseCase = FakeGetDocumentCommentsUseCase(),
        createCommentUseCase = FakeCreateDocumentCommentUseCase(),
        resolveCommentUseCase = FakeResolveDocumentCommentUseCase(),
        deleteCommentUseCase = FakeDeleteDocumentCommentUseCase(),
        updatePresenceUseCase = FakeUpdatePresenceUseCase(updatePresenceResult),
        getUserPresenceUseCase = FakeGetUserPresenceUseCase(getUserPresenceResult),
        setOfflineUseCase = FakeSetOfflineUseCase(setOfflineResult),
        getCollaborationUrlUseCase = FakeGetCollaborationUrlUseCase(),
        getCurrentUserUseCase = FakeGetCurrentUserUseCase(),
        initialItemId = initialItemId,
    )

    @Test
    fun loadSession_success_setsSession() = ViewModelTestBase.runTestWithMain {
        val session = testSession(id = "s1", itemId = "i1")
        val vm = createViewModel(getSessionResult = Result.success(session))
        vm.loadSession("s1")
        assertEquals(session, vm.session)
        assertNull(vm.error)
    }

    @Test
    fun loadSession_error_setsError() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getSessionResult = Result.error("ERR", "Session not found"))
        vm.loadSession("s1")
        assertNull(vm.session)
        assertEquals("Session not found", vm.error)
    }

    @Test
    fun loadParticipants_success_afterLoadSession_setsParticipants() = ViewModelTestBase.runTestWithMain {
        val session = testSession()
        val participants = listOf(testParticipant())
        val getSession = FakeGetCollaborationSessionUseCase(Result.success(session))
        val getParticipants = FakeGetSessionParticipantsUseCase(Result.success(participants))
        val vm = CollaborationViewModel(
            joinSessionUseCase = FakeJoinCollaborationSessionUseCase(),
            leaveSessionUseCase = FakeLeaveCollaborationSessionUseCase(),
            getSessionUseCase = getSession,
            getParticipantsUseCase = getParticipants,
            getDocumentStateUseCase = FakeGetDocumentStateUseCase(),
            saveDocumentUseCase = FakeSaveDocumentUseCase(),
            getCommentsUseCase = FakeGetDocumentCommentsUseCase(),
            createCommentUseCase = FakeCreateDocumentCommentUseCase(),
            resolveCommentUseCase = FakeResolveDocumentCommentUseCase(),
            deleteCommentUseCase = FakeDeleteDocumentCommentUseCase(),
            updatePresenceUseCase = FakeUpdatePresenceUseCase(),
            getUserPresenceUseCase = FakeGetUserPresenceUseCase(),
            setOfflineUseCase = FakeSetOfflineUseCase(),
            getCollaborationUrlUseCase = FakeGetCollaborationUrlUseCase(),
            getCurrentUserUseCase = FakeGetCurrentUserUseCase(),
            initialItemId = "",
        )
        vm.loadSession("sess-1")
        vm.loadParticipants()
        assertEquals(participants, vm.participants)
        assertNull(vm.error)
    }

    @Test
    fun leaveSession_success_afterLoadSession_clearsSession() = ViewModelTestBase.runTestWithMain {
        val session = testSession()
        val vm = createViewModel(getSessionResult = Result.success(session))
        vm.loadSession("sess-1")
        assertEquals(session, vm.session)
        vm.leaveSession()
        assertNull(vm.session)
    }

    @Test
    fun leaveSession_error_setsError() = ViewModelTestBase.runTestWithMain {
        val session = testSession()
        val vm = createViewModel(
            getSessionResult = Result.success(session),
            leaveSessionResult = Result.error("ERR", "Leave failed"),
        )
        vm.loadSession("sess-1")
        vm.leaveSession()
        assertEquals("Leave failed", vm.error)
    }

    @Test
    fun updatePresence_success_clearsError() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(updatePresenceResult = Result.success(Unit))
        vm.updatePresence(PresenceStatus.ONLINE)
        assertNull(vm.error)
    }

    @Test
    fun updatePresence_error_setsError() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(updatePresenceResult = Result.error("ERR", "Presence failed"))
        vm.updatePresence(PresenceStatus.AWAY)
        assertEquals("Presence failed", vm.error)
    }

    @Test
    fun loadUserPresences_success_setsUserPresences() = ViewModelTestBase.runTestWithMain {
        val presences = listOf(testUserPresence())
        val vm = createViewModel(getUserPresenceResult = Result.success(presences))
        vm.loadUserPresences(listOf("user-1"))
        assertEquals(presences, vm.userPresences)
    }

    @Test
    fun setOffline_success_clearsError() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(setOfflineResult = Result.success(Unit))
        vm.setOffline()
        assertNull(vm.error)
    }

    @Test
    fun setOffline_error_setsError() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(setOfflineResult = Result.error("ERR", "Set offline failed"))
        vm.setOffline()
        assertEquals("Set offline failed", vm.error)
    }

    @Test
    fun clearError_clearsErrorMessage() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getSessionResult = Result.error("ERR", "Oops"))
        vm.loadSession("s1")
        assertEquals("Oops", vm.error)
        vm.clearError()
        assertNull(vm.error)
    }
}
