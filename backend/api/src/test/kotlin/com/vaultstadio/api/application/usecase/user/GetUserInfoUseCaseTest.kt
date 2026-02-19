/**
 * GetUserInfoUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.user

import arrow.core.Either
import com.vaultstadio.core.domain.model.UserInfo
import com.vaultstadio.core.domain.model.UserRole
import com.vaultstadio.core.domain.model.UserStatus
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetUserInfoUseCaseTest {

    private val userService: UserService = mockk()
    private val useCase = GetUserInfoUseCaseImpl(userService)

    @Test
    fun invokeReturnsUserInfoWhenServiceSucceeds() = runTest {
        val now = Clock.System.now()
        val info = UserInfo(
            id = "user-1",
            email = "u@test.com",
            username = "u",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            quotaBytes = null,
            avatarUrl = null,
            createdAt = now,
        )
        coEvery { userService.getUserInfo("user-1") } returns Either.Right(info)

        val result = useCase("user-1")

        assertTrue(result.isRight())
        assertEquals("user-1", (result as Either.Right).value.id)
    }

    @Test
    fun invokeReturnsLeftWhenServiceFails() = runTest {
        coEvery { userService.getUserInfo("user-1") } returns
            Either.Left(ItemNotFoundException(itemId = "user-1"))

        val result = useCase("user-1")

        assertTrue(result.isLeft())
    }
}
