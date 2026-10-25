/**
 * ChangePasswordUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.user

import arrow.core.Either
import com.vaultstadio.application.usecase.user.ChangePasswordUseCaseImpl
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChangePasswordUseCaseTest {

    private val userService: UserService = mockk()
    private val useCase = ChangePasswordUseCaseImpl(userService)

    @Test
    fun invokeReturnsRightWhenServiceSucceeds() = runTest {
        coEvery {
            userService.changePassword("user-1", "old", "new")
        } returns Either.Right(Unit)

        val result = useCase("user-1", "old", "new")

        assertTrue(result.isRight())
    }

    @Test
    fun invokeReturnsLeftWhenServiceFails() = runTest {
        coEvery {
            userService.changePassword("user-1", "wrong", "new")
        } returns Either.Left(ItemNotFoundException(itemId = "user"))

        val result = useCase("user-1", "wrong", "new")

        assertTrue(result.isLeft())
    }
}
