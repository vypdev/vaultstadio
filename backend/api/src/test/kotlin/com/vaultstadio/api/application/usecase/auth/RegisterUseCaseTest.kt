/**
 * RegisterUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.auth

import arrow.core.Either
import com.vaultstadio.application.usecase.auth.RegisterUseCaseImpl
import com.vaultstadio.core.domain.service.RegisterUserInput
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.auth.model.User
import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.domain.auth.model.UserStatus
import com.vaultstadio.domain.common.exception.ValidationException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegisterUseCaseTest {

    private val userService: UserService = mockk()
    private val useCase = RegisterUseCaseImpl(userService)

    @Test
    fun `invoke delegates to userService and returns Right User`() = runTest {
        val input = RegisterUserInput(
            email = "u@test.com",
            username = "user1",
            password = "Pass1234",
        )
        val user = User(
            id = "id-1",
            email = input.email,
            username = input.username,
            passwordHash = "hash",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )
        coEvery { userService.register(input) } returns Either.Right(user)

        val result = useCase(input)

        assertTrue(result.isRight())
        assertTrue((result as Either.Right<User>).value.id == user.id)
    }

    @Test
    fun `invoke returns Left when userService returns Left`() = runTest {
        val input = RegisterUserInput(
            email = "bad",
            username = "u",
            password = "short",
        )
        coEvery { userService.register(input) } returns Either.Left(ValidationException("Invalid email"))

        val result = useCase(input)

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left<*>).value is ValidationException)
    }
}
