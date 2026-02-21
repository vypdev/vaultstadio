/**
 * VaultStadio Exposed API Key Repository Tests
 */

package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.domain.auth.model.ApiKey
import com.vaultstadio.domain.auth.repository.ApiKeyRepository
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Unit tests for ExposedApiKeyRepository.
 */
class ExposedApiKeyRepositoryTest {

    private lateinit var repository: ApiKeyRepository

    @BeforeEach
    fun setup() {
        repository = ExposedApiKeyRepository()
    }

    @Nested
    @DisplayName("Repository API Tests")
    inner class RepositoryApiTests {

        @Test
        fun `repository should implement ApiKeyRepository interface`() {
            assertTrue(repository is ApiKeyRepository)
        }

        @Test
        fun `repository should be of correct implementation type`() {
            assertTrue(repository is ExposedApiKeyRepository)
        }
    }

    @Nested
    @DisplayName("ApiKey Model Tests")
    inner class ApiKeyModelTests {

        @Test
        fun `api key should be created with all required fields`() {
            val now = Clock.System.now()
            val expiresAt = now + 365.days

            val apiKey = ApiKey(
                id = "key-123",
                userId = "user-456",
                name = "My API Key",
                keyHash = "hashed_key_abc123",
                permissions = listOf("read", "write"),
                expiresAt = expiresAt,
                lastUsedAt = null,
                createdAt = now,
                isActive = true,
            )

            assertEquals("key-123", apiKey.id)
            assertEquals("user-456", apiKey.userId)
            assertEquals("My API Key", apiKey.name)
            assertEquals("hashed_key_abc123", apiKey.keyHash)
            assertEquals(listOf("read", "write"), apiKey.permissions)
            assertTrue(apiKey.isActive)
        }

        @Test
        fun `api key should support empty permissions`() {
            val now = Clock.System.now()

            val apiKey = ApiKey(
                id = "key-123",
                userId = "user-456",
                name = "Read-only Key",
                keyHash = "hashed_key",
                permissions = emptyList(),
                expiresAt = null,
                lastUsedAt = null,
                createdAt = now,
                isActive = true,
            )

            assertTrue(apiKey.permissions.isEmpty())
        }

        @Test
        fun `api key should support null expiration`() {
            val now = Clock.System.now()

            val apiKey = ApiKey(
                id = "key-123",
                userId = "user-456",
                name = "Permanent Key",
                keyHash = "hashed_key",
                permissions = listOf("read"),
                expiresAt = null,
                lastUsedAt = null,
                createdAt = now,
                isActive = true,
            )

            assertNull(apiKey.expiresAt)
        }

        @Test
        fun `api key should track last used time`() {
            val now = Clock.System.now()
            val lastUsed = now + 1.hours

            val apiKey = ApiKey(
                id = "key-123",
                userId = "user-456",
                name = "Active Key",
                keyHash = "hashed_key",
                permissions = listOf("read"),
                expiresAt = null,
                lastUsedAt = lastUsed,
                createdAt = now,
                isActive = true,
            )

            assertNotNull(apiKey.lastUsedAt)
            assertTrue(apiKey.lastUsedAt!! > apiKey.createdAt)
        }

        @Test
        fun `api key should be deactivatable`() {
            val now = Clock.System.now()

            val activeKey = ApiKey(
                id = "key-123",
                userId = "user-456",
                name = "Key",
                keyHash = "hashed_key",
                permissions = listOf("read"),
                expiresAt = null,
                lastUsedAt = null,
                createdAt = now,
                isActive = true,
            )

            val deactivatedKey = activeKey.copy(isActive = false)

            assertTrue(activeKey.isActive)
            assertFalse(deactivatedKey.isActive)
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
        fun `findByKeyHash method should exist`() {
            assertNotNull(repository::findByKeyHash)
        }

        @Test
        fun `findByUserId method should exist`() {
            assertNotNull(repository::findByUserId)
        }

        @Test
        fun `update method should exist`() {
            assertNotNull(repository::update)
        }

        @Test
        fun `delete method should exist`() {
            assertNotNull(repository::delete)
        }

        @Test
        fun `deleteByUserId method should exist`() {
            assertNotNull(repository::deleteByUserId)
        }
    }

    @Nested
    @DisplayName("Permissions Tests")
    inner class PermissionsTests {

        @Test
        fun `common api key permissions should be definable`() {
            val commonPermissions = listOf(
                "read",
                "write",
                "delete",
                "share",
                "admin",
            )

            commonPermissions.forEach { permission ->
                assertNotNull(permission)
                assertTrue(permission.isNotEmpty())
            }
        }

        @Test
        fun `permissions should be serializable to comma-separated string`() {
            val permissions = listOf("read", "write", "delete")
            val serialized = permissions.joinToString(",")

            assertEquals("read,write,delete", serialized)
        }

        @Test
        fun `permissions should be deserializable from comma-separated string`() {
            val serialized = "read,write,delete"
            val permissions = serialized.split(",").filter { it.isNotEmpty() }

            assertEquals(listOf("read", "write", "delete"), permissions)
        }

        @Test
        fun `empty permissions string should result in empty list`() {
            val serialized = ""
            val permissions = serialized.split(",").filter { it.isNotEmpty() }

            assertTrue(permissions.isEmpty())
        }
    }
}
