/**
 * VaultStadio File Version Model Tests
 */

package com.vaultstadio.core.domain.model

import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileVersionTest {

    @Test
    fun `FileVersion should have correct properties`() {
        val now = Clock.System.now()
        val version = FileVersion(
            id = "version-1",
            itemId = "item-1",
            versionNumber = 1,
            size = 1024,
            checksum = "abc123",
            storageKey = "storage/v1/file",
            createdBy = "user-1",
            createdAt = now,
            comment = "Initial version",
            isLatest = true,
        )

        assertEquals("version-1", version.id)
        assertEquals("item-1", version.itemId)
        assertEquals(1, version.versionNumber)
        assertEquals(1024, version.size)
        assertEquals("abc123", version.checksum)
        assertEquals("storage/v1/file", version.storageKey)
        assertEquals("user-1", version.createdBy)
        assertEquals("Initial version", version.comment)
        assertTrue(version.isLatest)
        assertNull(version.restoredFrom)
        assertFalse(version.isRestore)
    }

    @Test
    fun `FileVersion isRestore should be true when restoredFrom is set`() {
        val version = FileVersion(
            id = "version-2",
            itemId = "item-1",
            versionNumber = 3,
            size = 1024,
            checksum = "xyz789",
            storageKey = "storage/v3/file",
            createdBy = "user-1",
            createdAt = Clock.System.now(),
            restoredFrom = 1,
        )

        assertTrue(version.isRestore)
        assertEquals(1, version.restoredFrom)
    }

    @Test
    fun `VersionRetentionPolicy should have default values`() {
        val policy = VersionRetentionPolicy.DEFAULT

        assertEquals(10, policy.maxVersions)
        assertEquals(90, policy.maxAgeDays)
        assertEquals(1, policy.minVersionsToKeep)
        assertTrue(policy.excludePatterns.isEmpty())
    }

    @Test
    fun `VersionRetentionPolicy KEEP_ALL should have no limits`() {
        val policy = VersionRetentionPolicy.KEEP_ALL

        assertNull(policy.maxVersions)
        assertNull(policy.maxAgeDays)
        assertEquals(1, policy.minVersionsToKeep)
    }

    @Test
    fun `VersionDiff should track changes between versions`() {
        val diff = VersionDiff(
            fromVersion = 1,
            toVersion = 2,
            sizeChange = 512,
            additions = 10,
            deletions = 5,
            isBinary = false,
            patches = emptyList(),
        )

        assertEquals(1, diff.fromVersion)
        assertEquals(2, diff.toVersion)
        assertEquals(512, diff.sizeChange)
        assertEquals(10, diff.additions)
        assertEquals(5, diff.deletions)
        assertFalse(diff.isBinary)
    }

    @Test
    fun `DiffPatch should represent a single change`() {
        val patch = DiffPatch(
            operation = DiffOperation.ADD,
            startLine = 10,
            endLine = 15,
            oldContent = null,
            newContent = "new code",
        )

        assertEquals(DiffOperation.ADD, patch.operation)
        assertEquals(10, patch.startLine)
        assertEquals(15, patch.endLine)
        assertNull(patch.oldContent)
        assertEquals("new code", patch.newContent)
    }

    @Test
    fun `DiffOperation should have all required values`() {
        val operations = DiffOperation.entries

        assertTrue(operations.contains(DiffOperation.ADD))
        assertTrue(operations.contains(DiffOperation.DELETE))
        assertTrue(operations.contains(DiffOperation.MODIFY))
        assertTrue(operations.contains(DiffOperation.CONTEXT))
    }
}
