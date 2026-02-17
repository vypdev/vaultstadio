/**
 * VaultStadio User Repository Tests
 *
 * Integration tests for ExposedUserRepository using Testcontainers PostgreSQL.
 */

package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import com.vaultstadio.core.domain.model.User
import com.vaultstadio.core.domain.model.UserRole
import com.vaultstadio.core.domain.model.UserStatus
import com.vaultstadio.core.domain.repository.UserQuery
import com.vaultstadio.infrastructure.persistence.entities.ApiKeysTable
import com.vaultstadio.infrastructure.persistence.entities.UserSessionsTable
import com.vaultstadio.infrastructure.persistence.entities.UsersTable
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExposedUserRepositoryTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine").apply {
            withDatabaseName("vaultstadio_test")
            withUsername("test")
            withPassword("test")
        }
    }

    private lateinit var userRepository: ExposedUserRepository
    private lateinit var database: Database

    @BeforeAll
    fun setupDatabase() {
        postgres.start()
        database = Database.connect(
            url = postgres.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgres.username,
            password = postgres.password,
        )

        transaction(database) {
            SchemaUtils.create(UsersTable, UserSessionsTable, ApiKeysTable)
        }

        userRepository = ExposedUserRepository()
    }

    @AfterAll
    fun teardown() {
        postgres.stop()
    }

    @BeforeEach
    fun cleanup() {
        transaction(database) {
            UsersTable.deleteAll()
        }
    }

    @Nested
    inner class CreateTests {

        @Test
        fun `should create user successfully`() = runTest {
            // Given
            val user = createTestUser()

            // When
            val result = userRepository.create(user)

            // Then
            assertTrue(result.isRight())
            val created = (result as Either.Right).value
            assertEquals(user.email, created.email)
            assertEquals(user.username, created.username)
        }

        @Test
        fun `should persist user to database`() = runTest {
            // Given
            val user = createTestUser()
            userRepository.create(user)

            // When
            val result = userRepository.findById(user.id)

            // Then
            assertTrue(result.isRight())
            val found = (result as Either.Right).value
            assertNotNull(found)
            assertEquals(user.email, found.email)
        }
    }

    @Nested
    inner class FindTests {

        @Test
        fun `should find user by ID`() = runTest {
            // Given
            val user = createTestUser()
            userRepository.create(user)

            // When
            val result = userRepository.findById(user.id)

            // Then
            assertTrue(result.isRight())
            val found = (result as Either.Right).value
            assertNotNull(found)
            assertEquals(user.id, found.id)
        }

        @Test
        fun `should return null when user not found`() = runTest {
            // When
            val result = userRepository.findById("nonexistent-id")

            // Then
            assertTrue(result.isRight())
            val found = (result as Either.Right).value
            assertEquals(null, found)
        }

        @Test
        fun `should find user by email`() = runTest {
            // Given
            val user = createTestUser()
            userRepository.create(user)

            // When
            val result = userRepository.findByEmail(user.email)

            // Then
            assertTrue(result.isRight())
            val found = (result as Either.Right).value
            assertNotNull(found)
            assertEquals(user.email, found.email)
        }

        @Test
        fun `should find user by username`() = runTest {
            // Given
            val user = createTestUser()
            userRepository.create(user)

            // When
            val result = userRepository.findByUsername(user.username)

            // Then
            assertTrue(result.isRight())
            val found = (result as Either.Right).value
            assertNotNull(found)
            assertEquals(user.username, found.username)
        }
    }

    @Nested
    inner class UpdateTests {

        @Test
        fun `should update user successfully`() = runTest {
            // Given
            val user = createTestUser()
            userRepository.create(user)

            val updatedUser = user.copy(
                username = "newusername",
                updatedAt = Clock.System.now(),
            )

            // When
            val result = userRepository.update(updatedUser)

            // Then
            assertTrue(result.isRight())

            val found = userRepository.findById(user.id)
            assertTrue(found.isRight())
            assertEquals("newusername", (found as Either.Right).value?.username)
        }

        @Test
        fun `should update user quota`() = runTest {
            // Given
            val user = createTestUser()
            userRepository.create(user)

            val newQuota = 50L * 1024 * 1024 * 1024 // 50GB
            val updatedUser = user.copy(
                quotaBytes = newQuota,
                updatedAt = Clock.System.now(),
            )

            // When
            userRepository.update(updatedUser)

            // Then
            val found = userRepository.findById(user.id)
            assertTrue(found.isRight())
            assertEquals(newQuota, (found as Either.Right).value?.quotaBytes)
        }
    }

    @Nested
    inner class DeleteTests {

        @Test
        fun `should delete user successfully`() = runTest {
            // Given
            val user = createTestUser()
            userRepository.create(user)

            // When
            val result = userRepository.delete(user.id)

            // Then
            assertTrue(result.isRight())

            val found = userRepository.findById(user.id)
            assertTrue(found.isRight())
            assertEquals(null, (found as Either.Right).value)
        }
    }

    @Nested
    inner class ExistsTests {

        @Test
        fun `should return true when email exists`() = runTest {
            // Given
            val user = createTestUser()
            userRepository.create(user)

            // When
            val result = userRepository.existsByEmail(user.email)

            // Then
            assertTrue(result.isRight())
            assertEquals(true, (result as Either.Right).value)
        }

        @Test
        fun `should return false when email does not exist`() = runTest {
            // When
            val result = userRepository.existsByEmail("nonexistent@example.com")

            // Then
            assertTrue(result.isRight())
            assertEquals(false, (result as Either.Right).value)
        }

        @Test
        fun `should return true when username exists`() = runTest {
            // Given
            val user = createTestUser()
            userRepository.create(user)

            // When
            val result = userRepository.existsByUsername(user.username)

            // Then
            assertTrue(result.isRight())
            assertEquals(true, (result as Either.Right).value)
        }
    }

    @Nested
    inner class QueryTests {

        @Test
        fun `should count all users`() = runTest {
            // Given
            userRepository.create(createTestUser("user1@test.com", "user1"))
            userRepository.create(createTestUser("user2@test.com", "user2"))
            userRepository.create(createTestUser("user3@test.com", "user3"))

            // When
            val result = userRepository.countAll()

            // Then
            assertTrue(result.isRight())
            assertEquals(3, (result as Either.Right).value)
        }

        @Test
        fun `should query users with pagination`() = runTest {
            // Given
            repeat(5) { i ->
                userRepository.create(createTestUser("user$i@test.com", "user$i"))
            }

            // When
            val result = userRepository.query(UserQuery(limit = 3, offset = 0))

            // Then
            assertTrue(result.isRight())
            val paged = (result as Either.Right).value
            assertEquals(3, paged.items.size)
            assertEquals(5, paged.total)
        }
    }

    // Helper function
    private fun createTestUser(
        email: String = "test@example.com",
        username: String = "testuser",
    ): User {
        val now = Clock.System.now()
        return User(
            id = UUID.randomUUID().toString(),
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
}
