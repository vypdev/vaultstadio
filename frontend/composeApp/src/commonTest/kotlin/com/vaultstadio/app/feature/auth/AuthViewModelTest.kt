/**
 * Unit tests for AuthViewModel: form state updates and synchronous validation.
 * Use cases are faked; async login/register outcomes are covered by AuthUseCaseTest.
 */

package com.vaultstadio.app.feature.auth

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.auth.AuthRepository
import com.vaultstadio.app.domain.auth.model.LoginResult
import com.vaultstadio.app.domain.auth.model.StorageQuota
import com.vaultstadio.app.domain.auth.model.User
import com.vaultstadio.app.domain.auth.model.UserRole
import com.vaultstadio.app.data.auth.usecase.LoginUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.RegisterUseCaseImpl
import com.vaultstadio.app.feature.ViewModelTestBase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testUser() = User(
    id = "u1",
    email = "u@test.com",
    username = "user",
    role = UserRole.USER,
    avatarUrl = null,
    createdAt = testInstant,
)

private fun testLoginResult() = LoginResult(
    user = testUser(),
    token = "token",
    expiresAt = testInstant,
    refreshToken = "refresh",
)

private class FakeAuthRepository(
    var loginResult: Result<LoginResult> = Result.success(testLoginResult()),
    var registerResult: Result<User> = Result.success(testUser()),
) : AuthRepository {
    private val _currentUserFlow = MutableStateFlow<User?>(null)
    override val currentUserFlow: StateFlow<User?> = _currentUserFlow.asStateFlow()

    override suspend fun login(email: String, password: String): Result<LoginResult> {
        loginResult.onSuccess { _currentUserFlow.value = it.user }
        return loginResult
    }

    override suspend fun register(email: String, username: String, password: String): Result<User> =
        registerResult

    override suspend fun logout(): Result<Unit> = Result.success(Unit)
    override suspend fun getCurrentUser(): Result<User> = Result.success(testUser())
    override suspend fun refreshCurrentUser() {}
    override suspend fun getQuota(): Result<StorageQuota> =
        Result.success(
            StorageQuota(
                usedBytes = 0,
                quotaBytes = null,
                usagePercentage = 0.0,
                fileCount = 0,
                folderCount = 0,
                remainingBytes = null,
            ),
        )
    override suspend fun updateProfile(username: String?, avatarUrl: String?) = Result.success(testUser())
    override suspend fun changePassword(currentPassword: String, newPassword: String) = Result.success(Unit)
    override fun isLoggedIn(): Boolean = false
}

class AuthViewModelTest {

    private fun createViewModel(
        loginResult: Result<LoginResult> = Result.success(testLoginResult()),
        registerResult: Result<User> = Result.success(testUser()),
        onSuccess: () -> Unit = {},
    ): AuthViewModel {
        val repo = FakeAuthRepository(loginResult = loginResult, registerResult = registerResult)
        return AuthViewModel(
            loginUseCase = LoginUseCaseImpl(repo),
            registerUseCase = RegisterUseCaseImpl(repo),
            onSuccessCallback = AuthSuccessCallback { onSuccess() },
        )
    }

    @Test
    fun updateLoginEmail_updatesState() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        assertEquals("", vm.loginEmail)
        vm.updateLoginEmail("a@b.com")
        assertEquals("a@b.com", vm.loginEmail)
    }

    @Test
    fun updateLoginPassword_updatesState() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        assertEquals("", vm.loginPassword)
        vm.updateLoginPassword("secret")
        assertEquals("secret", vm.loginPassword)
    }

    @Test
    fun login_withBlankEmail_setsEmailPasswordRequired() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        vm.updateLoginPassword("pass")
        vm.login()
        assertTrue(vm.authError is AuthError.EmailPasswordRequired)
    }

    @Test
    fun login_withBlankPassword_setsEmailPasswordRequired() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        vm.updateLoginEmail("a@b.com")
        vm.login()
        assertTrue(vm.authError is AuthError.EmailPasswordRequired)
    }

    @Test
    fun register_withPasswordMismatch_setsPasswordsDoNotMatch() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        vm.updateRegisterEmail("a@b.com")
        vm.updateRegisterUsername("user")
        vm.updateRegisterPassword("password1")
        vm.updateRegisterConfirmPassword("password2")
        vm.register()
        assertTrue(vm.authError is AuthError.PasswordsDoNotMatch)
    }

    @Test
    fun register_withShortPassword_setsPasswordTooShort() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        vm.updateRegisterEmail("a@b.com")
        vm.updateRegisterUsername("user")
        vm.updateRegisterPassword("short")
        vm.updateRegisterConfirmPassword("short")
        vm.register()
        assertTrue(vm.authError is AuthError.PasswordTooShort)
    }

    @Test
    fun register_withBlankFields_setsAllFieldsRequired() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        vm.updateRegisterEmail("a@b.com")
        vm.register()
        assertTrue(vm.authError is AuthError.AllFieldsRequired)
    }

    @Test
    fun toggleRegister_flipsShowRegister() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        assertEquals(false, vm.showRegister)
        vm.toggleRegister()
        assertEquals(true, vm.showRegister)
        vm.toggleRegister()
        assertEquals(false, vm.showRegister)
    }

    @Test
    fun clearError_clearsAuthError() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        vm.updateLoginPassword("x")
        vm.login()
        assertTrue(vm.authError is AuthError.EmailPasswordRequired)
        vm.clearError()
        assertNull(vm.authError)
    }

    // Async login/register outcomes are covered by AuthUseCaseTest and integration tests.
    // ViewModel's viewModelScope may use a different dispatcher, so we only test sync validation above.
}
