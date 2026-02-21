/**
 * RefreshSessionUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.auth

import arrow.core.Either
import com.vaultstadio.core.domain.model.User
import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.domain.auth.model.UserStatus
import com.vaultstadio.core.domain.service.RefreshResult
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RefreshSessionUseCaseTest {

    private val userService: UserService = mockk()
    private val useCase = RefreshSessionUseCaseImpl(userService)

    @Test
    fun invokeDelegatesToUserServiceAndReturnsRight() = runTest {
        val now = Clock.System.now()
        val user = User(
            id = "user-1",
            email = "u@test.com",
            username = "user1",
            passwordHash = "hash",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            createdAt = now,
            updatedAt = now,
        )
        val refreshResult = RefreshResult(
            user = user,
            sessionToken = "new-token",
            refreshToken = "new-refresh",
            expiresAt = now,
        )
        coEvery { userService.refreshSession("refresh-1") } returns Either.Right(refreshResult)

        val result = useCase("refresh-1")

        assertTrue(result.isRight())
        assertEquals("new-token", (result as Either.Right<*>).value.sessionToken)
    }

    @Test
    fun invokeReturnsLeftWhenServiceFails() = runTest {
        coEvery { userService.refreshSession("bad") } returns Either.Left(ItemNotFoundException(itemId = "token"))

        val result = useCase("bad")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left<*>).value is ItemNotFoundException)
    }
}
