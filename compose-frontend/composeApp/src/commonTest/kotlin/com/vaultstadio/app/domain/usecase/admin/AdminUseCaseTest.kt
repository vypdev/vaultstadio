/**
 * Unit tests for admin use cases (GetAdminUsers, UpdateUserQuota, UpdateUserRole, UpdateUserStatus).
 * Uses a fake AdminRepository to avoid platform/DI.
 */

package com.vaultstadio.app.domain.usecase.admin

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AdminRepository
import com.vaultstadio.app.domain.model.AdminUser
import com.vaultstadio.app.domain.model.PaginatedResponse
import com.vaultstadio.app.domain.model.UserRole
import com.vaultstadio.app.domain.model.UserStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testAdminUser(
    id: String = "admin-1",
    email: String = "admin@test.com",
    username: String = "admin",
    role: UserRole = UserRole.ADMIN,
    status: UserStatus = UserStatus.ACTIVE,
    quotaBytes: Long? = 10_000_000L,
) = AdminUser(
    id = id,
    email = email,
    username = username,
    role = role,
    status = status,
    avatarUrl = null,
    quotaBytes = quotaBytes,
    usedBytes = 1_000_000L,
    createdAt = testInstant,
    lastLoginAt = null,
)

private class FakeAdminRepository(
    var getUsersResult: ApiResult<PaginatedResponse<AdminUser>> = ApiResult.success(
        PaginatedResponse(emptyList(), 0L, 0, 50, 0, false),
    ),
    var updateUserQuotaResult: ApiResult<AdminUser> = ApiResult.success(testAdminUser()),
    var updateUserRoleResult: ApiResult<AdminUser> = ApiResult.success(testAdminUser()),
    var updateUserStatusResult: ApiResult<AdminUser> = ApiResult.success(testAdminUser()),
) : AdminRepository {

    override suspend fun getUsers(limit: Int, offset: Int): ApiResult<PaginatedResponse<AdminUser>> = getUsersResult

    override suspend fun updateUserQuota(userId: String, quotaBytes: Long?): ApiResult<AdminUser> =
        updateUserQuotaResult

    override suspend fun updateUserRole(userId: String, role: UserRole): ApiResult<AdminUser> = updateUserRoleResult

    override suspend fun updateUserStatus(userId: String, status: UserStatus): ApiResult<AdminUser> =
        updateUserStatusResult
}

class GetAdminUsersUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetUsersResult() = runTest {
        val users = listOf(testAdminUser("u1"), testAdminUser("u2"))
        val paged = PaginatedResponse(users, 2L, 0, 50, 1, false)
        val repo = FakeAdminRepository(getUsersResult = ApiResult.success(paged))
        val useCase = GetAdminUsersUseCaseImpl(repo)
        val result = useCase(limit = 50, offset = 0)
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.items?.size)
        assertEquals(2L, result.getOrNull()?.total)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeAdminRepository(getUsersResult = ApiResult.error("FORBIDDEN", "Admin only"))
        val useCase = GetAdminUsersUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class UpdateUserQuotaUseCaseTest {

    @Test
    fun invoke_returnsRepositoryUpdateUserQuotaResult() = runTest {
        val user = testAdminUser(id = "u3", quotaBytes = 20_000_000L)
        val repo = FakeAdminRepository(updateUserQuotaResult = ApiResult.success(user))
        val useCase = UpdateUserQuotaUseCaseImpl(repo)
        val result = useCase("u3", 20_000_000L)
        assertTrue(result.isSuccess())
        assertEquals(20_000_000L, result.getOrNull()?.quotaBytes)
    }

    @Test
    fun invoke_withNullQuota_forwardsToRepository() = runTest {
        val user = testAdminUser(quotaBytes = null)
        val repo = FakeAdminRepository(updateUserQuotaResult = ApiResult.success(user))
        val useCase = UpdateUserQuotaUseCaseImpl(repo)
        val result = useCase("u1", null)
        assertTrue(result.isSuccess())
    }
}

class UpdateUserRoleUseCaseTest {

    @Test
    fun invoke_returnsRepositoryUpdateUserRoleResult() = runTest {
        val user = testAdminUser(role = UserRole.USER)
        val repo = FakeAdminRepository(updateUserRoleResult = ApiResult.success(user))
        val useCase = UpdateUserRoleUseCaseImpl(repo)
        val result = useCase("u1", UserRole.USER)
        assertTrue(result.isSuccess())
        assertEquals(UserRole.USER, result.getOrNull()?.role)
    }
}

class UpdateUserStatusUseCaseTest {

    @Test
    fun invoke_returnsRepositoryUpdateUserStatusResult() = runTest {
        val user = testAdminUser(status = UserStatus.SUSPENDED)
        val repo = FakeAdminRepository(updateUserStatusResult = ApiResult.success(user))
        val useCase = UpdateUserStatusUseCaseImpl(repo)
        val result = useCase("u1", UserStatus.SUSPENDED)
        assertTrue(result.isSuccess())
        assertEquals(UserStatus.SUSPENDED, result.getOrNull()?.status)
    }
}
