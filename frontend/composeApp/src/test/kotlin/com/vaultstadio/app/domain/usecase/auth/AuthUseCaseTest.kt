/**
 * Unit tests for auth use cases (Login, Register, Logout, GetCurrentUser, GetQuota, UpdateProfile, ChangePassword).
 * Uses a fake AuthRepository to avoid platform/DI.
 */

package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.data.auth.usecase.ChangePasswordUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.GetCurrentUserUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.GetActiveSessionsUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.GetLoginHistoryUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.GetQuotaUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.GetSecuritySettingsUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.LoginUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.LogoutUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.RegisterUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.RevokeSessionUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.UpdateProfileUseCaseImpl
import com.vaultstadio.app.domain.auth.AuthRepository
import com.vaultstadio.app.domain.auth.model.LoginResult
import com.vaultstadio.app.domain.auth.model.StorageQuota
import com.vaultstadio.app.domain.auth.model.User
import com.vaultstadio.app.domain.auth.model.UserRole
import com.vaultstadio.app.domain.result.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testUser(
    id: String = "u1",
    email: String = "u@test.com",
    username: String = "user",
) = User(
    id = id,
    email = email,
    username = username,
    role = UserRole.USER,
    avatarUrl = null,
    createdAt = testInstant,
)

private fun testLoginResult(user: User = testUser()) = LoginResult(
    user = user,
    token = "token",
    expiresAt = testInstant,
    refreshToken = "refresh",
)

private fun testStorageQuota(
    usedBytes: Long = 0L,
    quotaBytes: Long? = null,
    usagePercentage: Double = 0.0,
    fileCount: Long = 0L,
    folderCount: Long = 0L,
) = StorageQuota(
    usedBytes = usedBytes,
    quotaBytes = quotaBytes,
    usagePercentage = usagePercentage,
    fileCount = fileCount,
    folderCount = folderCount,
    remainingBytes = quotaBytes?.let { it - usedBytes },
)

private class FakeAuthRepository(
    var loginResult: Result<LoginResult> = Result.success(testLoginResult()),
    var registerResult: Result<User> = Result.success(testUser()),
    var logoutResult: Result<Unit> = Result.success(Unit),
    var getCurrentUserResult: Result<User> = Result.success(testUser()),
    var getQuotaResult: Result<StorageQuota> = Result.success(testStorageQuota()),
    var updateProfileResult: Result<User> = Result.success(testUser()),
    var changePasswordResult: Result<Unit> = Result.success(Unit),
) : AuthRepository {

    private val _currentUserFlow = MutableStateFlow<User?>(null)
    override val currentUserFlow: StateFlow<User?> = _currentUserFlow.asStateFlow()

    override suspend fun login(email: String, password: String): Result<LoginResult> = loginResult

    override suspend fun register(email: String, username: String, password: String): Result<User> = registerResult

    override suspend fun logout(): Result<Unit> = logoutResult

    override suspend fun getCurrentUser(): Result<User> = getCurrentUserResult

    override suspend fun refreshCurrentUser() {}

    override suspend fun getQuota(): Result<StorageQuota> = getQuotaResult

    override suspend fun updateProfile(username: String?, avatarUrl: String?): Result<User> = updateProfileResult

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = changePasswordResult

    override fun isLoggedIn(): Boolean = _currentUserFlow.value != null
}

class LoginUseCaseTest {

    @Test
    fun invoke_returnsRepositoryLoginResult() = runTest {
        val loginResult = testLoginResult(testUser("u2", "a@test.com", "alice"))
        val repo = FakeAuthRepository(loginResult = Result.success(loginResult))
        val useCase = LoginUseCaseImpl(repo)
        val result = useCase("a@test.com", "password")
        assertTrue(result.isSuccess())
        assertEquals(loginResult, result.getOrNull())
        assertEquals("alice", result.getOrNull()?.user?.username)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeAuthRepository(loginResult = Result.error("INVALID_CREDENTIALS", "Bad credentials"))
        val useCase = LoginUseCaseImpl(repo)
        val result = useCase("u@test.com", "wrong")
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class RegisterUseCaseTest {

    @Test
    fun invoke_returnsRepositoryRegisterResult() = runTest {
        val user = testUser("new-1", "new@test.com", "newuser")
        val repo = FakeAuthRepository(registerResult = Result.success(user))
        val useCase = RegisterUseCaseImpl(repo)
        val result = useCase("new@test.com", "newuser", "secret123")
        assertTrue(result.isSuccess())
        assertEquals(user, result.getOrNull())
        assertEquals("newuser", result.getOrNull()?.username)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeAuthRepository(registerResult = Result.error("EMAIL_TAKEN", "Email already registered"))
        val useCase = RegisterUseCaseImpl(repo)
        val result = useCase("taken@test.com", "user", "pass")
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class LogoutUseCaseTest {

    @Test
    fun invoke_returnsRepositoryLogoutResult() = runTest {
        val repo = FakeAuthRepository(logoutResult = Result.success(Unit))
        val useCase = LogoutUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeAuthRepository(logoutResult = Result.error("NETWORK", "Logout failed"))
        val useCase = LogoutUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
    }
}

class GetCurrentUserUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetCurrentUserResult() = runTest {
        val user = testUser("current-1", "c@test.com", "currentuser")
        val repo = FakeAuthRepository(getCurrentUserResult = Result.success(user))
        val useCase = GetCurrentUserUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
        assertEquals(user, result.getOrNull())
        assertEquals("currentuser", result.getOrNull()?.username)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeAuthRepository(getCurrentUserResult = Result.error("UNAUTHORIZED", "Not logged in"))
        val useCase = GetCurrentUserUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class GetQuotaUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetQuotaResult() = runTest {
        val quota = testStorageQuota(usedBytes = 1024L, quotaBytes = 10_240L, fileCount = 5L, folderCount = 2L)
        val repo = FakeAuthRepository(getQuotaResult = Result.success(quota))
        val useCase = GetQuotaUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
        assertEquals(quota, result.getOrNull())
        assertEquals(1024L, result.getOrNull()?.usedBytes)
        assertEquals(5L, result.getOrNull()?.fileCount)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeAuthRepository(getQuotaResult = Result.error("UNAUTHORIZED", "Not logged in"))
        val useCase = GetQuotaUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class UpdateProfileUseCaseTest {

    @Test
    fun invoke_returnsRepositoryUpdateProfileResult() = runTest {
        val user = testUser("u1", "u@test.com", "newname")
        val repo = FakeAuthRepository(updateProfileResult = Result.success(user))
        val useCase = UpdateProfileUseCaseImpl(repo)
        val result = useCase("newname", null)
        assertTrue(result.isSuccess())
        assertEquals("newname", result.getOrNull()?.username)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeAuthRepository(updateProfileResult = Result.error("CONFLICT", "Username taken"))
        val useCase = UpdateProfileUseCaseImpl(repo)
        val result = useCase("taken", null)
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class ChangePasswordUseCaseTest {

    @Test
    fun invoke_returnsRepositoryChangePasswordResult() = runTest {
        val repo = FakeAuthRepository(changePasswordResult = Result.success(Unit))
        val useCase = ChangePasswordUseCaseImpl(repo)
        val result = useCase("old", "new")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeAuthRepository(changePasswordResult = Result.error("INVALID", "Current password wrong"))
        val useCase = ChangePasswordUseCaseImpl(repo)
        val result = useCase("wrong", "new")
        assertTrue(result.isError())
    }
}

class GetLoginHistoryUseCaseTest {

    @Test
    fun invoke_returnsEmptyList() = runTest {
        val useCase = GetLoginHistoryUseCaseImpl()
        val result = useCase()
        assertTrue(result.isSuccess())
        assertTrue(result.getOrNull().orEmpty().isEmpty())
    }
}

class RevokeSessionUseCaseTest {

    @Test
    fun invoke_returnsNotImplementedError() = runTest {
        val useCase = RevokeSessionUseCaseImpl()
        val result = useCase("session-1")
        assertTrue(result.isError())
    }
}

class GetSecuritySettingsUseCaseTest {

    @Test
    fun invoke_returnsDefaultSecuritySettings() = runTest {
        val useCase = GetSecuritySettingsUseCaseImpl()
        val result = useCase()
        assertTrue(result.isSuccess())
        assertEquals(false, result.getOrNull()?.twoFactorEnabled)
        assertNull(result.getOrNull()?.twoFactorMethod)
    }
}

class GetActiveSessionsUseCaseTest {

    @Test
    fun invoke_returnsEmptyList() = runTest {
        val useCase = GetActiveSessionsUseCaseImpl()
        val result = useCase()
        assertTrue(result.isSuccess())
        assertTrue(result.getOrNull().orEmpty().isEmpty())
    }
}
