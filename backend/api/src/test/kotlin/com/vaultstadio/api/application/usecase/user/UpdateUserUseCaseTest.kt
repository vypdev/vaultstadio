/**
 * UpdateUserUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.user

import arrow.core.Either
import com.vaultstadio.core.domain.model.User
import com.vaultstadio.core.domain.model.UserRole
import com.vaultstadio.core.domain.model.UserStatus
import com.vaultstadio.core.domain.service.UpdateUserInput
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateUserUseCaseTest {

    private val userService: UserService = mockk()
    private val useCase = UpdateUserUseCaseImpl(userService)

    @Test
    fun invokeReturnsUserWhenServiceSucceeds() = runTest {
        val now = Clock.System.now()
        val user = User(
            id = "user-1",
            email = "u@test.com",
            username = "newname",
            passwordHash = "h",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            createdAt = now,
            updatedAt = now,
        )
        val input = UpdateUserInput(userId = "user-1", username = "newname")
        coEvery { userService.updateUser(input) } returns Either.Right(user)

        val result = useCase(input)

        assertTrue(result.isRight())
        assertTrue((result as Either.Right).value.username == "newname")
    }

    @Test
    fun invokeReturnsLeftWhenServiceFails() = runTest {
        val input = UpdateUserInput(userId = "user-1", username = "x")
        coEvery { userService.updateUser(input) } returns
            Either.Left(ItemNotFoundException(itemId = "user-1"))

        val result = useCase(input)

        assertTrue(result.isLeft())
    }
}
