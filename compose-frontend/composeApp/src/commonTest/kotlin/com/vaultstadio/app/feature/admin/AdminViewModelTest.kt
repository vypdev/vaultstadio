/**
 * Unit tests for AdminViewModel: clearError and validation (invalid role/status).
 * Async loadUsers/updateUser* are covered by use case tests.
 */

package com.vaultstadio.app.feature.admin

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.model.AdminUser
import com.vaultstadio.app.domain.model.PaginatedResponse
import com.vaultstadio.app.domain.model.UserRole
import com.vaultstadio.app.domain.model.UserStatus
import com.vaultstadio.app.domain.usecase.admin.GetAdminUsersUseCase
import com.vaultstadio.app.domain.usecase.admin.UpdateUserQuotaUseCase
import com.vaultstadio.app.domain.usecase.admin.UpdateUserRoleUseCase
import com.vaultstadio.app.domain.usecase.admin.UpdateUserStatusUseCase
import com.vaultstadio.app.feature.ViewModelTestBase
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testAdminUser() = AdminUser(
    id = "u1",
    email = "a@b.com",
    username = "user",
    role = UserRole.USER,
    status = UserStatus.ACTIVE,
    avatarUrl = null,
    quotaBytes = 10_000_000L,
    usedBytes = 0L,
    createdAt = testInstant,
    lastLoginAt = null,
)

private class FakeGetAdminUsersUseCase(
    var result: ApiResult<PaginatedResponse<AdminUser>> = ApiResult.success(
        PaginatedResponse(emptyList(), 0L, 0, 50, 0, false),
    ),
) : GetAdminUsersUseCase {
    override suspend fun invoke(limit: Int, offset: Int): ApiResult<PaginatedResponse<AdminUser>> = result
}

private class FakeUpdateUserQuotaUseCase(
    var result: ApiResult<AdminUser> = ApiResult.success(testAdminUser()),
) : UpdateUserQuotaUseCase {
    override suspend fun invoke(userId: String, quotaBytes: Long?): ApiResult<AdminUser> = result
}

private class FakeUpdateUserRoleUseCase(
    var result: ApiResult<AdminUser> = ApiResult.success(testAdminUser()),
) : UpdateUserRoleUseCase {
    override suspend fun invoke(userId: String, role: UserRole): ApiResult<AdminUser> = result
}

private class FakeUpdateUserStatusUseCase(
    var result: ApiResult<AdminUser> = ApiResult.success(testAdminUser()),
) : UpdateUserStatusUseCase {
    override suspend fun invoke(userId: String, status: UserStatus): ApiResult<AdminUser> = result
}

class AdminViewModelTest {

    private fun createViewModel(
        getUsersResult: ApiResult<PaginatedResponse<AdminUser>> = ApiResult.success(
            PaginatedResponse(emptyList(), 0L, 0, 50, 0, false),
        ),
    ): AdminViewModel = AdminViewModel(
        getAdminUsersUseCase = FakeGetAdminUsersUseCase(getUsersResult),
        updateUserQuotaUseCase = FakeUpdateUserQuotaUseCase(),
        updateUserRoleUseCase = FakeUpdateUserRoleUseCase(),
        updateUserStatusUseCase = FakeUpdateUserStatusUseCase(),
    )

    @Test
    fun clearError_clearsErrorMessage() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getUsersResult = ApiResult.error("ERR", "Something failed"))
        vm.loadUsers()
        testScheduler.advanceUntilIdle()
        assertTrue(vm.error != null)
        vm.clearError()
        assertNull(vm.error)
    }

    @Test
    fun loadUsers_onSuccess_updatesUsers() = ViewModelTestBase.runTestWithMain {
        val users = listOf(testAdminUser())
        val vm = createViewModel(
            getUsersResult = ApiResult.success(PaginatedResponse(users, 1L, 0, 50, 1, false)),
        )
        vm.loadUsers()
        testScheduler.advanceUntilIdle()
        assertEquals(1, vm.users.size)
        assertEquals("u1", vm.users[0].id)
    }

    @Test
    fun clearError_doesNotThrow() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        vm.clearError()
        assertNull(vm.error)
    }
}
