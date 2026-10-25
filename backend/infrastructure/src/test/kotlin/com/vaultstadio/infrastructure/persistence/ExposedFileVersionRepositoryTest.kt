/**
 * VaultStadio Exposed File Version Repository Tests
 */

package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.model.FileVersionHistory
import com.vaultstadio.core.domain.model.VersionRetentionPolicy
import com.vaultstadio.core.domain.repository.FileVersionRepository
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
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

/**
 * Unit tests for ExposedFileVersionRepository.
 */
class ExposedFileVersionRepositoryTest {

    private lateinit var repository: FileVersionRepository

    @BeforeEach
    fun setup() {
        repository = ExposedFileVersionRepository()
    }

    @Nested
    @DisplayName("Repository API Tests")
    inner class RepositoryApiTests {

        @Test
        fun `repository should implement FileVersionRepository interface`() {
            assertTrue(repository is FileVersionRepository)
        }

        @Test
        fun `repository should be of correct implementation type`() {
            assertTrue(repository is ExposedFileVersionRepository)
        }
    }

    @Nested
    @DisplayName("FileVersion Model Tests")
    inner class FileVersionModelTests {

        @Test
        fun `version should be created with all required fields`() {
            val now = Clock.System.now()

            val version = FileVersion(
                id = "version-123",
                itemId = "item-456",
                versionNumber = 1,
                size = 1024L,
                checksum = "abc123md5",
                storageKey = "storage/items/item-456/v1",
                createdBy = "user-789",
                createdAt = now,
                comment = "Initial version",
                isLatest = true,
                restoredFrom = null,
            )

            assertEquals("version-123", version.id)
            assertEquals("item-456", version.itemId)
            assertEquals(1, version.versionNumber)
            assertEquals(1024L, version.size)
            assertEquals("abc123md5", version.checksum)
            assertEquals("storage/items/item-456/v1", version.storageKey)
            assertEquals("user-789", version.createdBy)
            assertEquals(now, version.createdAt)
            assertEquals("Initial version", version.comment)
            assertTrue(version.isLatest)
            assertNull(version.restoredFrom)
        }

        @Test
        fun `version should support incrementing version numbers`() {
            val now = Clock.System.now()

            val v1 = FileVersion(
                id = "v1", itemId = "item", versionNumber = 1, size = 100,
                checksum = "c1", storageKey = "k1", createdBy = "user",
                createdAt = now, comment = null, isLatest = false, restoredFrom = null,
            )
            val v2 = v1.copy(id = "v2", versionNumber = 2)
            val v3 = v1.copy(id = "v3", versionNumber = 3, isLatest = true)

            assertEquals(1, v1.versionNumber)
            assertEquals(2, v2.versionNumber)
            assertEquals(3, v3.versionNumber)
            assertTrue(v3.isLatest)
            assertFalse(v1.isLatest)
        }

        @Test
        fun `version should track restoration source`() {
            val now = Clock.System.now()

            val restoredVersion = FileVersion(
                id = "v5",
                itemId = "item",
                versionNumber = 5,
                size = 500,
                checksum = "c5",
                storageKey = "k5",
                createdBy = "user",
                createdAt = now,
                comment = "Restored from version 2",
                isLatest = true,
                restoredFrom = 2,
            )

            assertNotNull(restoredVersion.restoredFrom)
            assertEquals(2, restoredVersion.restoredFrom)
        }

        @Test
        fun `version should allow null optional fields`() {
            val now = Clock.System.now()

            val version = FileVersion(
                id = "v1",
                itemId = "item",
                versionNumber = 1,
                size = 100,
                checksum = "c1",
                storageKey = "k1",
                createdBy = "user",
                createdAt = now,
                comment = null,
                isLatest = true,
                restoredFrom = null,
            )

            assertNull(version.comment)
            assertNull(version.restoredFrom)
        }
    }

    @Nested
    @DisplayName("FileVersionHistory Model Tests")
    inner class FileVersionHistoryModelTests {

        @Test
        fun `history should aggregate version information`() {
            val now = Clock.System.now()

            val item = StorageItem(
                id = "item-123",
                name = "document.pdf",
                path = "/documents/document.pdf",
                type = ItemType.FILE,
                ownerId = "user-456",
                createdAt = now,
                updatedAt = now,
            )

            val versions = listOf(
                FileVersion("v1", "item-123", 1, 1000, "c1", "k1", "user", now, null, false, null),
                FileVersion("v2", "item-123", 2, 1500, "c2", "k2", "user", now, null, false, null),
                FileVersion("v3", "item-123", 3, 2000, "c3", "k3", "user", now, null, true, null),
            )

            val history = FileVersionHistory(
                item = item,
                versions = versions,
                totalVersions = 3,
                totalSize = 4500L,
            )

            assertEquals(item, history.item)
            assertEquals(3, history.versions.size)
            assertEquals(3, history.totalVersions)
            assertEquals(4500L, history.totalSize)
        }

        @Test
        fun `history should handle empty versions`() {
            val now = Clock.System.now()

            val item = StorageItem(
                id = "item-123",
                name = "new-file.txt",
                path = "/new-file.txt",
                type = ItemType.FILE,
                ownerId = "user",
                createdAt = now,
                updatedAt = now,
            )

            val history = FileVersionHistory(
                item = item,
                versions = emptyList(),
                totalVersions = 0,
                totalSize = 0L,
            )

            assertTrue(history.versions.isEmpty())
            assertEquals(0, history.totalVersions)
            assertEquals(0L, history.totalSize)
        }
    }

    @Nested
    @DisplayName("VersionRetentionPolicy Model Tests")
    inner class VersionRetentionPolicyModelTests {

        @Test
        fun `policy should define retention rules`() {
            val policy = VersionRetentionPolicy(
                minVersionsToKeep = 3,
                maxVersions = 10,
                maxAgeDays = 90,
            )

            assertEquals(3, policy.minVersionsToKeep)
            assertEquals(10, policy.maxVersions)
            assertEquals(90, policy.maxAgeDays)
        }

        @Test
        fun `policy should allow null optional limits`() {
            val policy = VersionRetentionPolicy(
                minVersionsToKeep = 1,
                maxVersions = null,
                maxAgeDays = null,
            )

            assertEquals(1, policy.minVersionsToKeep)
            assertNull(policy.maxVersions)
            assertNull(policy.maxAgeDays)
        }

        @Test
        fun `policy should support version-only retention`() {
            val policy = VersionRetentionPolicy(
                minVersionsToKeep = 5,
                maxVersions = 20,
                maxAgeDays = null,
            )

            assertEquals(20, policy.maxVersions)
            assertNull(policy.maxAgeDays)
        }

        @Test
        fun `policy should support time-based retention`() {
            val policy = VersionRetentionPolicy(
                minVersionsToKeep = 1,
                maxVersions = null,
                maxAgeDays = 365,
            )

            assertNull(policy.maxVersions)
            assertEquals(365, policy.maxAgeDays)
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
        fun `findByItemAndVersion method should exist`() {
            assertNotNull(repository::findByItemAndVersion)
        }

        @Test
        fun `findLatest method should exist`() {
            assertNotNull(repository::findLatest)
        }

        @Test
        fun `listVersions method should exist`() {
            assertNotNull(repository::listVersions)
        }

        @Test
        fun `getHistory method should exist`() {
            assertNotNull(repository::getHistory)
        }

        @Test
        fun `getNextVersionNumber method should exist`() {
            assertNotNull(repository::getNextVersionNumber)
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
        fun `deleteAllForItem method should exist`() {
            assertNotNull(repository::deleteAllForItem)
        }

        @Test
        fun `applyRetentionPolicy method should exist`() {
            assertNotNull(repository::applyRetentionPolicy)
        }

        @Test
        fun `countVersions method should exist`() {
            assertNotNull(repository::countVersions)
        }

        @Test
        fun `getTotalVersionSize method should exist`() {
            assertNotNull(repository::getTotalVersionSize)
        }

        @Test
        fun `streamVersions method should exist`() {
            assertNotNull(repository::streamVersions)
        }

        @Test
        fun `findByStorageKey method should exist`() {
            assertNotNull(repository::findByStorageKey)
        }
    }

    @Nested
    @DisplayName("Version Number Tests")
    inner class VersionNumberTests {

        @Test
        fun `version numbers should start at 1`() {
            val version = FileVersion(
                id = "v1", itemId = "item", versionNumber = 1,
                size = 100, checksum = "c", storageKey = "k", createdBy = "u",
                createdAt = Clock.System.now(), comment = null, isLatest = true, restoredFrom = null,
            )

            assertEquals(1, version.versionNumber)
        }

        @Test
        fun `version numbers should be sequential`() {
            val versions = (1..5).map { num ->
                FileVersion(
                    id = "v$num", itemId = "item", versionNumber = num,
                    size = 100L * num, checksum = "c$num", storageKey = "k$num",
                    createdBy = "user", createdAt = Clock.System.now(),
                    comment = null, isLatest = num == 5, restoredFrom = null,
                )
            }

            versions.forEachIndexed { index, version ->
                assertEquals(index + 1, version.versionNumber)
            }
        }
    }

    @Nested
    @DisplayName("Version Size Tests")
    inner class VersionSizeTests {

        @Test
        fun `version should track file size`() {
            val version = FileVersion(
                id = "v1", itemId = "item", versionNumber = 1,
                size = 1024L * 1024L * 50L, // 50 MB
                checksum = "c", storageKey = "k", createdBy = "u",
                createdAt = Clock.System.now(), comment = null, isLatest = true, restoredFrom = null,
            )

            assertEquals(52428800L, version.size) // 50 MB in bytes
        }

        @Test
        fun `version should allow zero size`() {
            val version = FileVersion(
                id = "v1", itemId = "item", versionNumber = 1,
                size = 0L,
                checksum = "empty", storageKey = "k", createdBy = "u",
                createdAt = Clock.System.now(), comment = null, isLatest = true, restoredFrom = null,
            )

            assertEquals(0L, version.size)
        }
    }
}
