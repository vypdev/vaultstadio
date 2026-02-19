/**
 * LogoutUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.auth

import arrow.core.Either
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LogoutUseCaseTest {

    private val userService: UserService = mockk()
    private val useCase = LogoutUseCaseImpl(userService)

    @Test
    fun invokeDelegatesToUserServiceAndReturnsRight() = runTest {
        coEvery { userService.logout("token-1") } returns Either.Right(Unit)

        val result = useCase("token-1")

        assertTrue(result.isRight())
    }

    @Test
    fun invokeReturnsLeftWhenServiceFails() = runTest {
        coEvery { userService.logout("token-1") } returns Either.Left(ItemNotFoundException(itemId = "session"))

        val result = useCase("token-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }
}
