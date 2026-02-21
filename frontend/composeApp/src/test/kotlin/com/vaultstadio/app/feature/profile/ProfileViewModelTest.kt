/**
 * Unit tests for ProfileViewModel: clearError and clearSuccessMessage.
 * loadProfile, updateProfile, changePassword, exportData are async and covered by use case tests.
 */

package com.vaultstadio.app.feature.profile

import com.vaultstadio.app.data.auth.usecase.ChangePasswordUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.GetCurrentUserUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.GetQuotaUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.UpdateProfileUseCaseImpl
import com.vaultstadio.app.domain.auth.AuthRepository
import com.vaultstadio.app.domain.auth.model.LoginResult
import com.vaultstadio.app.domain.auth.model.StorageQuota
import com.vaultstadio.app.domain.auth.model.User
import com.vaultstadio.app.domain.auth.model.UserRole
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.feature.ViewModelTestBase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertNull

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testUser() = User(
    id = "u1",
    email = "u@test.com",
    username = "user",
    role = UserRole.USER,
    avatarUrl = null,
    createdAt = testInstant,
)

private fun testQuota() = StorageQuota(
    usedBytes = 0,
    quotaBytes = null,
    usagePercentage = 0.0,
    fileCount = 0,
    folderCount = 0,
    remainingBytes = null,
)

private class FakeAuthRepositoryForProfile : AuthRepository {
    private val _currentUserFlow = MutableStateFlow<User?>(testUser())
    override val currentUserFlow: StateFlow<User?> = _currentUserFlow.asStateFlow()

    override suspend fun login(email: String, password: String): Result<LoginResult> =
        Result.error("", "")
    override suspend fun register(email: String, username: String, password: String): Result<User> =
        Result.error("", "")
    override suspend fun logout() = Result.success(Unit)
    override suspend fun getCurrentUser() = Result.success(testUser())
    override suspend fun refreshCurrentUser() {}
    override suspend fun getQuota(): Result<StorageQuota> = Result.success(testQuota())
    override suspend fun updateProfile(username: String?, avatarUrl: String?) = Result.success(testUser())
    override suspend fun changePassword(currentPassword: String, newPassword: String) = Result.success(Unit)
    override fun isLoggedIn() = true
}

private class StubProfileComponent : ProfileComponent {
    override fun onBack() {}
    override fun navigateToChangePassword() {}
    override fun navigateToSecurity() {}
    override fun exportData(fileName: String, data: ByteArray, mimeType: String) {}
}

class ProfileViewModelTest {

    private fun createViewModel(): ProfileViewModel {
        val repo = FakeAuthRepositoryForProfile()
        return ProfileViewModel(
            getCurrentUserUseCase = GetCurrentUserUseCaseImpl(repo),
            getQuotaUseCase = GetQuotaUseCaseImpl(repo),
            updateProfileUseCase = UpdateProfileUseCaseImpl(repo),
            changePasswordUseCase = ChangePasswordUseCaseImpl(repo),
            profileComponent = StubProfileComponent(),
        )
    }

    @Test
    fun clearError_clearsError() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        vm.clearError()
        assertNull(vm.error)
    }

    @Test
    fun clearSuccessMessage_clearsSuccessMessage() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        vm.clearSuccessMessage()
        assertNull(vm.successMessage)
    }
}
