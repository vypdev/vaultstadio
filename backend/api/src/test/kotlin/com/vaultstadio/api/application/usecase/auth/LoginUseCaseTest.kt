/**
 * LoginUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.auth

import arrow.core.Either
import com.vaultstadio.application.usecase.auth.LoginUseCaseImpl
import com.vaultstadio.core.domain.service.LoginInput
import com.vaultstadio.core.domain.service.LoginResult
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.auth.model.User
import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.domain.auth.model.UserStatus
import com.vaultstadio.domain.common.exception.AuthenticationException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoginUseCaseTest {

    private val userService: UserService = mockk()
    private val useCase = LoginUseCaseImpl(userService)

    @Test
    fun `invoke delegates to userService and returns Right LoginResult`() = runTest {
        val input = LoginInput(
            email = "u@test.com",
            password = "Pass1234",
            ipAddress = null,
            userAgent = null,
        )
        val user = User(
            id = "id-1",
            email = input.email,
            username = "user1",
            passwordHash = "hash",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )
        val loginResult = LoginResult(
            user = user,
            sessionToken = "token",
            refreshToken = "refresh",
            expiresAt = Clock.System.now(),
        )
        coEvery { userService.login(input) } returns Either.Right(loginResult)

        val result = useCase(input)

        assertTrue(result.isRight())
        assertTrue((result as Either.Right<LoginResult>).value.sessionToken == "token")
    }

    @Test
    fun `invoke returns Left when userService returns Left`() = runTest {
        val input = LoginInput("u@test.com", "wrong", null, null)
        coEvery { userService.login(input) } returns Either.Left(AuthenticationException("Invalid credentials"))

        val result = useCase(input)

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left<*>).value is AuthenticationException)
    }
}
