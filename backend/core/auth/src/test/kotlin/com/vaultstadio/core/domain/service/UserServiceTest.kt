/**
 * VaultStadio User Service Tests
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.event.UserEvent
import com.vaultstadio.domain.auth.model.User
import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.domain.auth.model.UserSession
import com.vaultstadio.domain.auth.model.UserStatus
import com.vaultstadio.domain.auth.repository.SessionRepository
import com.vaultstadio.domain.auth.repository.UserQuery
import com.vaultstadio.domain.auth.repository.UserRepository
import com.vaultstadio.domain.common.exception.AuthenticationException
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.common.exception.ValidationException
import com.vaultstadio.domain.common.pagination.PagedResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var passwordHasher: PasswordHasher
    private lateinit var eventBus: EventBus
    private lateinit var userService: UserService

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        sessionRepository = mockk(relaxed = true)
        passwordHasher = mockk()
        eventBus = mockk(relaxed = true)
        userService = UserService(userRepository, sessionRepository, passwordHasher, eventBus)
    }

    @Nested
    inner class RegistrationTests {

        @Test
        fun `should register user successfully`() = runTest {
            // Given
            val input = RegisterUserInput(
                email = "test@example.com",
                username = "testuser",
                password = "password123",
            )

            coEvery { userRepository.existsByEmail(any()) } returns false.right()
            coEvery { userRepository.existsByUsername(any()) } returns false.right()
            every { passwordHasher.hash(any()) } returns "hashed_password"
            coEvery { userRepository.create(any()) } answers { firstArg<User>().right() }

            // When
            val result = userService.register(input)

            // Then
            assertTrue(result.isRight())
            val user = (result as Either.Right).value
            assertEquals("test@example.com", user.email)
            assertEquals("testuser", user.username)
            assertEquals("hashed_password", user.passwordHash)
            assertEquals(UserRole.USER, user.role)
            assertEquals(UserStatus.ACTIVE, user.status)

            coVerify { eventBus.publish(any<UserEvent.Created>()) }
        }

        @Test
        fun `should fail with invalid email`() = runTest {
            // Given
            val input = RegisterUserInput(
                email = "invalid-email",
                username = "testuser",
                password = "password123",
            )

            // When
            val result = userService.register(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ValidationException)
            assertTrue(result.value.message?.contains("email") == true)
        }

        @Test
        fun `should fail with weak password`() = runTest {
            // Given
            val input = RegisterUserInput(
                email = "test@example.com",
                username = "testuser",
                password = "short",
            )

            // When
            val result = userService.register(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ValidationException)
            assertTrue(result.value.message?.contains("Password") == true)
        }

        @Test
        fun `should fail with password without numbers`() = runTest {
            // Given
            val input = RegisterUserInput(
                email = "test@example.com",
                username = "testuser",
                password = "longpassword",
            )

            // When
            val result = userService.register(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ValidationException)
        }

        @Test
        fun `should fail when email already exists`() = runTest {
            // Given
            val input = RegisterUserInput(
                email = "existing@example.com",
                username = "testuser",
                password = "password123",
            )

            coEvery { userRepository.existsByEmail("existing@example.com") } returns true.right()

            // When
            val result = userService.register(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ValidationException)
            assertTrue(result.value.message?.contains("Email") == true)
        }

        @Test
        fun `should fail when username already exists`() = runTest {
            // Given
            val input = RegisterUserInput(
                email = "test@example.com",
                username = "existinguser",
                password = "password123",
            )

            coEvery { userRepository.existsByEmail(any()) } returns false.right()
            coEvery { userRepository.existsByUsername("existinguser") } returns true.right()

            // When
            val result = userService.register(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ValidationException)
            assertTrue(result.value.message?.contains("Username") == true)
        }

        @Test
        fun `should propagate when existsByEmail returns Left`() = runTest {
            val input = RegisterUserInput(
                email = "test@example.com",
                username = "testuser",
                password = "password123",
            )
            val repoError = ItemNotFoundException("Database unavailable")
            coEvery { userRepository.existsByEmail(any()) } returns repoError.left()

            val result = userService.register(input)

            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ItemNotFoundException)
        }

        @Test
        fun `should propagate when existsByUsername returns Left`() = runTest {
            val input = RegisterUserInput(
                email = "test@example.com",
                username = "testuser",
                password = "password123",
            )
            coEvery { userRepository.existsByEmail(any()) } returns false.right()
            val repoError = ItemNotFoundException("Database error")
            coEvery { userRepository.existsByUsername(any()) } returns repoError.left()

            val result = userService.register(input)

            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ItemNotFoundException)
        }
    }

    @Nested
    inner class LoginTests {

        @Test
        fun `should login successfully`() = runTest {
            // Given
            val user = createTestUser("user-123", "test@example.com", "testuser")
            val input = LoginInput(
                email = "test@example.com",
                password = "password123",
                ipAddress = "127.0.0.1",
                userAgent = "Test Agent",
            )

            coEvery { userRepository.findByEmail("test@example.com") } returns user.right()
            every { passwordHasher.verify("password123", user.passwordHash) } returns true
            coEvery { sessionRepository.create(any()) } returns mockk<UserSession>().right()
            coEvery { userRepository.updateLastLogin(any()) } returns Unit.right()

            // When
            val result = userService.login(input)

            // Then
            assertTrue(result.isRight())
            val loginResult = (result as Either.Right).value
            assertEquals(user.id, loginResult.user.id)
            assertNotNull(loginResult.sessionToken)
            assertNotNull(loginResult.expiresAt)

            coVerify { eventBus.publish(any<UserEvent.LoggedIn>()) }
        }

        @Test
        fun `should fail with invalid email`() = runTest {
            // Given
            val input = LoginInput(
                email = "notexist@example.com",
                password = "password123",
                ipAddress = null,
                userAgent = null,
            )

            coEvery { userRepository.findByEmail("notexist@example.com") } returns null.right()

            // When
            val result = userService.login(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is AuthenticationException)
        }

        @Test
        fun `should fail with invalid password`() = runTest {
            // Given
            val user = createTestUser("user-123", "test@example.com", "testuser")
            val input = LoginInput(
                email = "test@example.com",
                password = "wrongpassword",
                ipAddress = null,
                userAgent = null,
            )

            coEvery { userRepository.findByEmail("test@example.com") } returns user.right()
            every { passwordHasher.verify("wrongpassword", user.passwordHash) } returns false

            // When
            val result = userService.login(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is AuthenticationException)
        }

        @Test
        fun `should fail when account is suspended`() = runTest {
            // Given
            val user = createTestUser("user-123", "test@example.com", "testuser")
                .copy(status = UserStatus.SUSPENDED)
            val input = LoginInput(
                email = "test@example.com",
                password = "password123",
                ipAddress = null,
                userAgent = null,
            )

            coEvery { userRepository.findByEmail("test@example.com") } returns user.right()
            every { passwordHasher.verify("password123", user.passwordHash) } returns true

            // When
            val result = userService.login(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is AuthorizationException)
        }
    }

    @Nested
    inner class SessionTests {

        @Test
        fun `should validate valid session`() = runTest {
            // Given
            val user = createTestUser("user-123", "test@example.com", "testuser")
            val session = createTestSession("session-id", "user-123")
            val token = "valid-token"

            coEvery { sessionRepository.findByTokenHash(any()) } returns session.right()
            coEvery { userRepository.findById("user-123") } returns user.right()
            coEvery { sessionRepository.updateLastActivity(any()) } returns Unit.right()

            // When
            val result = userService.validateSession(token)

            // Then
            assertTrue(result.isRight())
            assertEquals(user.id, (result as Either.Right).value.id)
        }

        @Test
        fun `should fail with invalid session token`() = runTest {
            // Given
            val token = "invalid-token"

            coEvery { sessionRepository.findByTokenHash(any()) } returns null.right()

            // When
            val result = userService.validateSession(token)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is AuthenticationException)
        }

        @Test
        fun `should fail when session is expired`() = runTest {
            val pastExpiry = kotlinx.datetime.Instant.DISTANT_PAST
            val session = createTestSession("session-id", "user-123").copy(expiresAt = pastExpiry)
            val user = createTestUser("user-123", "test@example.com", "testuser")
            coEvery { sessionRepository.findByTokenHash(any()) } returns session.right()
            coEvery { userRepository.findById("user-123") } returns user.right()
            coEvery { sessionRepository.delete("session-id") } returns Unit.right()

            val result = userService.validateSession("any-token")

            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is AuthenticationException)
            assertTrue(result.value.message?.contains("expired") == true)
        }

        @Test
        fun `should propagate when findByTokenHash returns Left`() = runTest {
            val repoError = ItemNotFoundException("Session store error")
            coEvery { sessionRepository.findByTokenHash(any()) } returns repoError.left()

            val result = userService.validateSession("any-token")

            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ItemNotFoundException)
        }

        @Test
        fun `should logout successfully`() = runTest {
            // Given
            val session = createTestSession("session-id", "user-123")
            val token = "valid-token"

            coEvery { sessionRepository.findByTokenHash(any()) } returns session.right()
            coEvery { sessionRepository.delete("session-id") } returns Unit.right()

            // When
            val result = userService.logout(token)

            // Then
            assertTrue(result.isRight())
            coVerify { sessionRepository.delete("session-id") }
        }

        @Test
        fun `should logout all sessions for user`() = runTest {
            // Given
            val userId = "user-123"

            coEvery { sessionRepository.deleteByUserId(userId) } returns Unit.right()

            // When
            val result = userService.logoutAll(userId)

            // Then
            assertTrue(result.isRight())
            coVerify { sessionRepository.deleteByUserId(userId) }
        }
    }

    @Nested
    inner class PasswordChangeTests {

        @Test
        fun `should change password successfully`() = runTest {
            // Given
            val user = createTestUser("user-123", "test@example.com", "testuser")

            coEvery { userRepository.findById("user-123") } returns user.right()
            every { passwordHasher.verify("oldpassword1", user.passwordHash) } returns true
            every { passwordHasher.hash("newpassword1") } returns "new_hashed_password"
            coEvery { userRepository.update(any()) } answers { firstArg<User>().right() }
            coEvery { sessionRepository.deleteByUserId("user-123") } returns Unit.right()

            // When
            val result = userService.changePassword("user-123", "oldpassword1", "newpassword1")

            // Then
            assertTrue(result.isRight())
            coVerify { sessionRepository.deleteByUserId("user-123") }
        }

        @Test
        fun `should fail with wrong current password`() = runTest {
            // Given
            val user = createTestUser("user-123", "test@example.com", "testuser")

            coEvery { userRepository.findById("user-123") } returns user.right()
            every { passwordHasher.verify("wrongpassword", user.passwordHash) } returns false

            // When
            val result = userService.changePassword("user-123", "wrongpassword", "newpassword1")

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is AuthenticationException)
        }

        @Test
        fun `should fail with weak new password`() = runTest {
            // Given
            val user = createTestUser("user-123", "test@example.com", "testuser")

            coEvery { userRepository.findById("user-123") } returns user.right()
            every { passwordHasher.verify("oldpassword1", user.passwordHash) } returns true

            // When
            val result = userService.changePassword("user-123", "oldpassword1", "weak")

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ValidationException)
        }
    }

    @Nested
    inner class AdminTests {

        @Test
        fun `should list users as admin`() = runTest {
            // Given
            val admin = createTestUser("admin-123", "admin@example.com", "admin")
                .copy(role = UserRole.ADMIN)
            val users = listOf(
                createTestUser("user-1", "user1@example.com", "user1"),
                createTestUser("user-2", "user2@example.com", "user2"),
            )
            val query = UserQuery()

            coEvery { userRepository.findById("admin-123") } returns admin.right()
            coEvery { userRepository.query(query) } returns PagedResult(
                items = users,
                total = 2,
                limit = 20,
                offset = 0,
            ).right()

            // When
            val result = userService.listUsers("admin-123", query)

            // Then
            assertTrue(result.isRight())
            assertEquals(2, (result as Either.Right).value.items.size)
        }

        @Test
        fun `should fail listing users as non-admin`() = runTest {
            // Given
            val user = createTestUser("user-123", "user@example.com", "user")
            val query = UserQuery()

            coEvery { userRepository.findById("user-123") } returns user.right()

            // When
            val result = userService.listUsers("user-123", query)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is AuthorizationException)
        }

        @Test
        fun `should update quota as admin`() = runTest {
            // Given
            val admin = createTestUser("admin-123", "admin@example.com", "admin")
                .copy(role = UserRole.ADMIN)
            val user = createTestUser("user-123", "user@example.com", "user")
            val newQuota = 50L * 1024 * 1024 * 1024 // 50GB

            coEvery { userRepository.findById("admin-123") } returns admin.right()
            coEvery { userRepository.findById("user-123") } returns user.right()
            coEvery { userRepository.update(any()) } answers { firstArg<User>().right() }

            // When
            val result = userService.updateQuota("user-123", newQuota, "admin-123")

            // Then
            assertTrue(result.isRight())
            assertEquals(newQuota, (result as Either.Right).value.quotaBytes)
        }

        @Test
        fun `should delete user as admin`() = runTest {
            // Given
            val admin = createTestUser("admin-123", "admin@example.com", "admin")
                .copy(role = UserRole.ADMIN)

            coEvery { userRepository.findById("admin-123") } returns admin.right()
            coEvery { sessionRepository.deleteByUserId("user-123") } returns Unit.right()
            coEvery { userRepository.delete("user-123") } returns Unit.right()

            // When
            val result = userService.deleteUser("user-123", "admin-123")

            // Then
            assertTrue(result.isRight())
            coVerify { sessionRepository.deleteByUserId("user-123") }
            coVerify { userRepository.delete("user-123") }
        }
    }

    // Helper functions to create test data

    private fun createTestUser(
        id: String,
        email: String,
        username: String,
    ): User {
        val now = Clock.System.now()
        return User(
            id = id,
            email = email,
            username = username,
            passwordHash = "hashed_password",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            quotaBytes = 10L * 1024 * 1024 * 1024,
            avatarUrl = null,
            preferences = null,
            lastLoginAt = null,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun createTestSession(
        id: String,
        userId: String,
    ): UserSession {
        val now = Clock.System.now()
        return UserSession(
            id = id,
            userId = userId,
            tokenHash = "token_hash",
            ipAddress = "127.0.0.1",
            userAgent = "Test Agent",
            expiresAt = now + kotlin.time.Duration.parse("7d"),
            createdAt = now,
            lastActivityAt = now,
        )
    }
}
