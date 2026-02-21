/**
 * DeleteUserUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.admin

import arrow.core.Either
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeleteUserUseCaseTest {

    private val userService: UserService = mockk()
    private val useCase = DeleteUserUseCaseImpl(userService)

    @Test
    fun invokeReturnsRightWhenServiceSucceeds() = runTest {
        coEvery { userService.deleteUser("user-1", "admin-1") } returns Either.Right(Unit)

        val result = useCase("user-1", "admin-1")

        assertTrue(result.isRight())
    }

    @Test
    fun invokeReturnsLeftWhenServiceFails() = runTest {
        coEvery { userService.deleteUser("user-1", "admin-1") } returns
            Either.Left(ItemNotFoundException(itemId = "user-1"))

        val result = useCase("user-1", "admin-1")

        assertTrue(result.isLeft())
    }
}
