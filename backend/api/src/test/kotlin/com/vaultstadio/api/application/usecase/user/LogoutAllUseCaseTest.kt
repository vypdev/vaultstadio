/**
 * LogoutAllUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.user

import arrow.core.Either
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LogoutAllUseCaseTest {

    private val userService: UserService = mockk()
    private val useCase = LogoutAllUseCaseImpl(userService)

    @Test
    fun invokeReturnsRightWhenServiceSucceeds() = runTest {
        coEvery { userService.logoutAll("user-1") } returns Either.Right(Unit)

        val result = useCase("user-1")

        assertTrue(result.isRight())
    }

    @Test
    fun invokeReturnsLeftWhenServiceFails() = runTest {
        coEvery { userService.logoutAll("user-1") } returns
            Either.Left(ItemNotFoundException(itemId = "x"))

        val result = useCase("user-1")

        assertTrue(result.isLeft())
    }
}
