/**
 * ListUsersUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.admin

import arrow.core.Either
import com.vaultstadio.core.domain.model.User
import com.vaultstadio.core.domain.model.UserRole
import com.vaultstadio.core.domain.model.UserStatus
import com.vaultstadio.core.domain.repository.PagedResult
import com.vaultstadio.core.domain.repository.UserQuery
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.core.exception.AuthorizationException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ListUsersUseCaseTest {

    private val userService: UserService = mockk()
    private val useCase = ListUsersUseCaseImpl(userService)

    @Test
    fun `invoke delegates to userService and returns Right PagedResult`() = runTest {
        val adminId = "admin-1"
        val query = UserQuery(limit = 10, offset = 0)
        val user = User(
            id = "u1",
            email = "u@test.com",
            username = "user1",
            passwordHash = "hash",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )
        val paged = PagedResult(
            items = listOf(user),
            total = 1,
            offset = 0,
            limit = 10,
        )
        coEvery { userService.listUsers(adminId, query) } returns Either.Right(paged)

        val result = useCase(adminId, query)

        assertTrue(result.isRight())
        assertTrue((result as Either.Right).value.items.size == 1)
    }

    @Test
    fun `invoke returns Left when not admin`() = runTest {
        coEvery { userService.listUsers(any(), any()) } returns
            Either.Left(AuthorizationException("Admin access required"))

        val result = useCase("user-1", UserQuery())

        assertTrue(result.isLeft())
    }
}
