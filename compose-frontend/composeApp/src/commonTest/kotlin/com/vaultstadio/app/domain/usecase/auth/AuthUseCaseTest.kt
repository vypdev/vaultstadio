/**
 * Unit tests for auth use cases (Login, Register, Logout, GetCurrentUser, GetQuota, UpdateProfile, ChangePassword).
 * Uses a fake AuthRepository to avoid platform/DI.
 */

package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AuthRepository
import com.vaultstadio.app.domain.model.LoginResult
import com.vaultstadio.app.domain.model.StorageQuota
import com.vaultstadio.app.domain.model.User
import com.vaultstadio.app.domain.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testUser(id: String = "u1", email: String = "u@test.com", username: String = "user") =
    User(
        id = id,
        email = email,
        username = username,
        role = UserRole.USER,
        avatarUrl = null,
        createdAt = testInstant,
    )

private fun testLoginResult(user: User = testUser(), token: String = "token") =
    LoginResult(
        user = user,
        token = token,
        expiresAt = testInstant,
        refreshToken = "refresh",
    )

private fun testQuota() =
    StorageQuota(
        usedBytes = 1000L,
        quotaBytes = 10_000L,
        usagePercentage = 10.0,
        fileCount = 5L,
        folderCount = 2L,
        remainingBytes = 9_000L,
    )

private class FakeAuthRepository(
    var loginResult: ApiResult<LoginResult> = ApiResult.success(testLoginResult()),
    var registerResult: ApiResult<User> = ApiResult.success(testUser()),
    var logoutResult: ApiResult<Unit> = ApiResult.success(Unit),
    var getCurrentUserResult: ApiResult<User> = ApiResult.success(testUser()),
    var getQuotaResult: ApiResult<StorageQuota> = ApiResult.success(testQuota()),
    var updateProfileResult: ApiResult<User> = ApiResult.success(testUser()),
    var changePasswordResult: ApiResult<Unit> = ApiResult.success(Unit),
    initialUser: User? = null,
    var isLoggedInValue: Boolean = false,
) : AuthRepository {

    private val _currentUserFlow = MutableStateFlow(initialUser)
    override val currentUserFlow: StateFlow<User?> = _currentUserFlow.asStateFlow()

    override suspend fun login(email: String, password: String): ApiResult<LoginResult> {
        loginResult.onSuccess { _currentUserFlow.value = it.user }
        return loginResult
    }

    override suspend fun register(email: String, username: String, password: String): ApiResult<User> =
        registerResult

    override suspend fun logout(): ApiResult<Unit> {
        _currentUserFlow.value = null
        return logoutResult
    }

    override suspend fun getCurrentUser(): ApiResult<User> = getCurrentUserResult

    override suspend fun refreshCurrentUser() {
        getCurrentUserResult.onSuccess { _currentUserFlow.value = it }
    }

    override suspend fun getQuota(): ApiResult<StorageQuota> = getQuotaResult

    override suspend fun updateProfile(username: String?, avatarUrl: String?): ApiResult<User> {
        updateProfileResult.onSuccess { _currentUserFlow.value = it }
        return updateProfileResult
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit> =
        changePasswordResult

    override fun isLoggedIn(): Boolean = isLoggedInValue
}

class LoginUseCaseTest {

    @Test
    fun invoke_returnsRepositoryLoginResult() = runTest {
        val loginResult = testLoginResult(user = testUser("u2"), token = "jwt")
        val repo = FakeAuthRepository(loginResult = ApiResult.success(loginResult))
        val useCase = LoginUseCaseImpl(repo)
        val result = useCase("a@b.com", "pass")
        assertTrue(result.isSuccess())
        assertEquals(loginResult, result.getOrNull())
        assertEquals("u2", repo.currentUserFlow.value?.id)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeAuthRepository(loginResult = ApiResult.error("AUTH_FAILED", "Invalid credentials"))
        val useCase = LoginUseCaseImpl(repo)
        val result = useCase("a@b.com", "wrong")
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class RegisterUseCaseTest {

    @Test
    fun invoke_returnsRepositoryRegisterResult() = runTest {
        val user = testUser("u3", username = "newuser")
        val repo = FakeAuthRepository(registerResult = ApiResult.success(user))
        val useCase = RegisterUseCaseImpl(repo)
        val result = useCase("new@test.com", "newuser", "secret")
        assertTrue(result.isSuccess())
        assertEquals(user, result.getOrNull())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeAuthRepository(registerResult = ApiResult.error("EMAIL_TAKEN", "Email already exists"))
        val useCase = RegisterUseCaseImpl(repo)
        val result = useCase("a@b.com", "user", "pass")
        assertTrue(result.isError())
    }
}

class LogoutUseCaseTest {

    @Test
    fun invoke_returnsRepositoryLogoutResult() = runTest {
        val repo = FakeAuthRepository(initialUser = testUser(), logoutResult = ApiResult.success(Unit))
        val useCase = LogoutUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
        assertNull(repo.currentUserFlow.value)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeAuthRepository(logoutResult = ApiResult.error("NETWORK", "Offline"))
        val useCase = LogoutUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
    }
}

class GetCurrentUserUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetCurrentUserResult() = runTest {
        val user = testUser("u4")
        val repo = FakeAuthRepository(getCurrentUserResult = ApiResult.success(user))
        val useCase = GetCurrentUserUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
        assertEquals(user, result.getOrNull())
    }

    @Test
    fun currentUserFlow_delegatesToRepository() = runTest {
        val user = testUser("u5")
        val repo = FakeAuthRepository(initialUser = user)
        val useCase = GetCurrentUserUseCaseImpl(repo)
        assertEquals(user, useCase.currentUserFlow.value)
    }

    @Test
    fun isLoggedIn_delegatesToRepository() {
        val repo = FakeAuthRepository(isLoggedInValue = true)
        val useCase = GetCurrentUserUseCaseImpl(repo)
        assertTrue(useCase.isLoggedIn())
        val repoNotLoggedIn = FakeAuthRepository(isLoggedInValue = false)
        val useCase2 = GetCurrentUserUseCaseImpl(repoNotLoggedIn)
        assertFalse(useCase2.isLoggedIn())
    }
}

class GetQuotaUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetQuotaResult() = runTest {
        val quota = testQuota()
        val repo = FakeAuthRepository(getQuotaResult = ApiResult.success(quota))
        val useCase = GetQuotaUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
        assertEquals(quota, result.getOrNull())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeAuthRepository(getQuotaResult = ApiResult.error("UNAUTHORIZED", "Not logged in"))
        val useCase = GetQuotaUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
    }
}

class UpdateProfileUseCaseTest {

    @Test
    fun invoke_returnsRepositoryUpdateProfileResult() = runTest {
        val user = testUser("u6", username = "updated")
        val repo = FakeAuthRepository(updateProfileResult = ApiResult.success(user))
        val useCase = UpdateProfileUseCaseImpl(repo)
        val result = useCase("updated", null)
        assertTrue(result.isSuccess())
        assertEquals(user, result.getOrNull())
        assertEquals("updated", repo.currentUserFlow.value?.username)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeAuthRepository(updateProfileResult = ApiResult.error("CONFLICT", "Username taken"))
        val useCase = UpdateProfileUseCaseImpl(repo)
        val result = useCase("taken", null)
        assertTrue(result.isError())
    }
}

class ChangePasswordUseCaseTest {

    @Test
    fun invoke_returnsRepositoryChangePasswordResult() = runTest {
        val repo = FakeAuthRepository(changePasswordResult = ApiResult.success(Unit))
        val useCase = ChangePasswordUseCaseImpl(repo)
        val result = useCase("old", "new")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeAuthRepository(changePasswordResult = ApiResult.error("BAD_REQUEST", "Wrong password"))
        val useCase = ChangePasswordUseCaseImpl(repo)
        val result = useCase("wrong", "new")
        assertTrue(result.isError())
    }
}
