/**
 * UpdateQuotaUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.admin

import arrow.core.Either
import com.vaultstadio.core.domain.model.User
import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.domain.auth.model.UserStatus
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateQuotaUseCaseTest {

    private val userService: UserService = mockk()
    private val useCase = UpdateQuotaUseCaseImpl(userService)

    @Test
    fun invokeReturnsUserWhenServiceSucceeds() = runTest {
        val now = Clock.System.now()
        val user = User(
            id = "user-1",
            email = "u@test.com",
            username = "u",
            passwordHash = "h",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            createdAt = now,
            updatedAt = now,
        )
        coEvery { userService.updateQuota("user-1", 1024L, "admin-1") } returns Either.Right(user)

        val result = useCase("user-1", 1024L, "admin-1")

        assertTrue(result.isRight())
    }

    @Test
    fun invokeReturnsLeftWhenServiceFails() = runTest {
        coEvery { userService.updateQuota("user-1", null, "admin-1") } returns
            Either.Left(ItemNotFoundException(itemId = "user-1"))

        val result = useCase("user-1", null, "admin-1")

        assertTrue(result.isLeft())
    }
}
