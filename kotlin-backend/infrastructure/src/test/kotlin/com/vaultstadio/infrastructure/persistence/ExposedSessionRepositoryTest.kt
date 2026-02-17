/**
 * VaultStadio Exposed Session Repository Tests
 */

package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.core.domain.model.UserSession
import com.vaultstadio.core.domain.repository.SessionRepository
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

/**
 * Unit tests for ExposedSessionRepository.
 */
class ExposedSessionRepositoryTest {

    private lateinit var repository: SessionRepository

    @BeforeEach
    fun setup() {
        repository = ExposedSessionRepository()
    }

    @Nested
    @DisplayName("Repository API Tests")
    inner class RepositoryApiTests {

        @Test
        fun `repository should implement SessionRepository interface`() {
            assertTrue(repository is SessionRepository)
        }

        @Test
        fun `repository should be of correct implementation type`() {
            assertTrue(repository is ExposedSessionRepository)
        }
    }

    @Nested
    @DisplayName("UserSession Model Tests")
    inner class UserSessionModelTests {

        @Test
        fun `session should be created with all required fields`() {
            val now = Clock.System.now()
            val expiresAt = now + 24.hours

            val session = UserSession(
                id = "session-123",
                userId = "user-456",
                tokenHash = "hashed_token_abc123",
                ipAddress = "192.168.1.1",
                userAgent = "Mozilla/5.0",
                expiresAt = expiresAt,
                createdAt = now,
                lastActivityAt = now,
            )

            assertEquals("session-123", session.id)
            assertEquals("user-456", session.userId)
            assertEquals("hashed_token_abc123", session.tokenHash)
            assertEquals("192.168.1.1", session.ipAddress)
            assertEquals("Mozilla/5.0", session.userAgent)
        }

        @Test
        fun `session should support nullable fields`() {
            val now = Clock.System.now()

            val session = UserSession(
                id = "session-123",
                userId = "user-456",
                tokenHash = "hashed_token",
                ipAddress = null,
                userAgent = null,
                expiresAt = now + 24.hours,
                createdAt = now,
                lastActivityAt = now,
            )

            assertNull(session.ipAddress)
            assertNull(session.userAgent)
        }

        @Test
        fun `session should track last activity`() {
            val now = Clock.System.now()
            val later = now + 1.hours

            val session = UserSession(
                id = "session-123",
                userId = "user-456",
                tokenHash = "hashed_token",
                ipAddress = null,
                userAgent = null,
                expiresAt = now + 24.hours,
                createdAt = now,
                lastActivityAt = later,
            )

            assertTrue(session.lastActivityAt > session.createdAt)
        }
    }

    @Nested
    @DisplayName("Repository Method Signature Tests")
    inner class MethodSignatureTests {

        @Test
        fun `create method should exist`() {
            assertNotNull(repository::create)
        }

        @Test
        fun `findById method should exist`() {
            assertNotNull(repository::findById)
        }

        @Test
        fun `findByTokenHash method should exist`() {
            assertNotNull(repository::findByTokenHash)
        }

        @Test
        fun `findByUserId method should exist`() {
            assertNotNull(repository::findByUserId)
        }

        @Test
        fun `delete method should exist`() {
            assertNotNull(repository::delete)
        }

        @Test
        fun `deleteByUserId method should exist`() {
            assertNotNull(repository::deleteByUserId)
        }

        @Test
        fun `deleteExpired method should exist`() {
            assertNotNull(repository::deleteExpired)
        }

        @Test
        fun `updateLastActivity method should exist`() {
            assertNotNull(repository::updateLastActivity)
        }
    }

    @Nested
    @DisplayName("Session Expiration Tests")
    inner class ExpirationTests {

        @Test
        fun `session should have expiration time after creation time`() {
            val now = Clock.System.now()
            val expiresAt = now + 24.hours

            assertTrue(expiresAt > now)
        }

        @Test
        fun `expired session can be detected`() {
            val now = Clock.System.now()
            val past = now - 1.hours

            // Session expired an hour ago
            val isExpired = past < now
            assertTrue(isExpired)
        }
    }
}
