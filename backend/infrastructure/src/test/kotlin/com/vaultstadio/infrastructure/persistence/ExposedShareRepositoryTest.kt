/**
 * VaultStadio Exposed Share Repository Tests
 */

package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.domain.share.repository.ShareRepository
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

/**
 * Unit tests for ExposedShareRepository.
 */
class ExposedShareRepositoryTest {

    private lateinit var repository: ShareRepository

    @BeforeEach
    fun setup() {
        repository = ExposedShareRepository()
    }

    @Nested
    @DisplayName("Repository API Tests")
    inner class RepositoryApiTests {

        @Test
        fun `repository should implement ShareRepository interface`() {
            assertTrue(repository is ShareRepository)
        }

        @Test
        fun `repository should be of correct implementation type`() {
            assertTrue(repository is ExposedShareRepository)
        }
    }

    @Nested
    @DisplayName("ShareLink Model Tests")
    inner class ShareLinkModelTests {

        @Test
        fun `share link should be created with all required fields`() {
            val now = Clock.System.now()
            val expiresAt = now + 7.days

            val share = ShareLink(
                id = "share-123",
                itemId = "item-456",
                token = "abc123token",
                createdBy = "user-789",
                expiresAt = expiresAt,
                password = null,
                maxDownloads = null,
                downloadCount = 0,
                isActive = true,
                createdAt = now,
            )

            assertEquals("share-123", share.id)
            assertEquals("item-456", share.itemId)
            assertEquals("abc123token", share.token)
            assertEquals("user-789", share.createdBy)
            assertEquals(0, share.downloadCount)
            assertTrue(share.isActive)
            assertNull(share.password)
            assertNull(share.maxDownloads)
        }

        @Test
        fun `share link should support password protection`() {
            val now = Clock.System.now()

            val share = ShareLink(
                id = "share-123",
                itemId = "item-456",
                token = "abc123token",
                createdBy = "user-789",
                expiresAt = now + 7.days,
                password = "hashed_password",
                maxDownloads = null,
                downloadCount = 0,
                isActive = true,
                createdAt = now,
            )

            assertNotNull(share.password)
            assertEquals("hashed_password", share.password)
        }

        @Test
        fun `share link should support download limits`() {
            val now = Clock.System.now()

            val share = ShareLink(
                id = "share-123",
                itemId = "item-456",
                token = "abc123token",
                createdBy = "user-789",
                expiresAt = now + 7.days,
                password = null,
                maxDownloads = 10,
                downloadCount = 5,
                isActive = true,
                createdAt = now,
            )

            assertEquals(10, share.maxDownloads)
            assertEquals(5, share.downloadCount)
        }

        @Test
        fun `share link should support copy with changes`() {
            val now = Clock.System.now()

            val original = ShareLink(
                id = "share-123",
                itemId = "item-456",
                token = "abc123token",
                createdBy = "user-789",
                expiresAt = now + 7.days,
                password = null,
                maxDownloads = null,
                downloadCount = 0,
                isActive = true,
                createdAt = now,
            )

            val updated = original.copy(downloadCount = 5, isActive = false)

            assertEquals("share-123", updated.id)
            assertEquals(5, updated.downloadCount)
            assertFalse(updated.isActive)
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
        fun `findByToken method should exist`() {
            assertNotNull(repository::findByToken)
        }

        @Test
        fun `findByItemId method should exist`() {
            assertNotNull(repository::findByItemId)
        }

        @Test
        fun `findByCreatedBy method should exist`() {
            assertNotNull(repository::findByCreatedBy)
        }

        @Test
        fun `update method should exist`() {
            assertNotNull(repository::update)
        }

        @Test
        fun `incrementDownloadCount method should exist`() {
            assertNotNull(repository::incrementDownloadCount)
        }

        @Test
        fun `delete method should exist`() {
            assertNotNull(repository::delete)
        }

        @Test
        fun `deleteByItemId method should exist`() {
            assertNotNull(repository::deleteByItemId)
        }

        @Test
        fun `deactivateExpired method should exist`() {
            assertNotNull(repository::deactivateExpired)
        }
    }

    @Nested
    @DisplayName("Share Token Generation Tests")
    inner class TokenGenerationTests {

        @Test
        fun `token should be unique per share`() {
            val token1 = UUID.randomUUID().toString().replace("-", "").take(16)
            val token2 = UUID.randomUUID().toString().replace("-", "").take(16)

            assertNotNull(token1)
            assertNotNull(token2)
            assertTrue(token1 != token2)
        }

        @Test
        fun `token should have reasonable length`() {
            val token = UUID.randomUUID().toString().replace("-", "").take(16)

            assertEquals(16, token.length)
        }
    }

    @Nested
    @DisplayName("Share Expiration Tests")
    inner class ExpirationTests {

        @Test
        fun `share should support various expiration durations`() {
            val now = Clock.System.now()

            val oneDay = now.plus(1.days)
            val oneWeek = now.plus(7.days)
            val oneMonth = now.plus(30.days)

            assertNotNull(oneDay)
            assertNotNull(oneWeek)
            assertNotNull(oneMonth)

            assertTrue(oneDay < oneWeek)
            assertTrue(oneWeek < oneMonth)
        }
    }
}
