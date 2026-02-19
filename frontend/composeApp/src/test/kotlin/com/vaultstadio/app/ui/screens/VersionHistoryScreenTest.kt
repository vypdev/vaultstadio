/**
 * VaultStadio Version History Screen Tests
 */

package com.vaultstadio.app.ui.screens

import com.vaultstadio.app.domain.version.model.FileVersion
import com.vaultstadio.app.domain.version.model.FileVersionHistory
import com.vaultstadio.app.domain.version.model.VersionDiff
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionHistoryScreenTest {

    @Test
    fun testFileVersionModel() {
        val now = Clock.System.now()
        val version = FileVersion(
            id = "v1",
            itemId = "item1",
            versionNumber = 3,
            size = 1024,
            checksum = "abc123",
            createdBy = "user1",
            createdAt = now,
            comment = null,
            isLatest = true,
            restoredFrom = null,
        )

        assertEquals(3, version.versionNumber)
        assertTrue(version.isLatest)
        assertFalse(version.isRestore)
    }

    @Test
    fun testFileVersionWithRestore() {
        val now = Clock.System.now()
        val version = FileVersion(
            id = "v2",
            itemId = "item1",
            versionNumber = 4,
            size = 1024,
            checksum = "def456",
            createdBy = "user1",
            createdAt = now,
            comment = "Restored version",
            isLatest = true,
            restoredFrom = 2,
        )

        assertTrue(version.isRestore)
        assertEquals(2, version.restoredFrom)
    }

    @Test
    fun testFileVersionHistoryModel() {
        val now = Clock.System.now()
        val versions = listOf(
            FileVersion("v1", "item1", 1, 1000, "abc", "user1", now, null, false, null),
            FileVersion("v2", "item1", 2, 1200, "def", "user1", now, null, true, null),
        )

        val history = FileVersionHistory(
            itemId = "item1",
            itemName = "document.txt",
            versions = versions,
            totalVersions = 2,
            totalSize = 2200,
        )

        assertEquals("document.txt", history.itemName)
        assertEquals(2, history.totalVersions)
        assertEquals(2200, history.totalSize)
    }

    @Test
    fun testVersionDiffModel() {
        val diff = VersionDiff(
            fromVersion = 1,
            toVersion = 2,
            sizeChange = 200,
            additions = 10,
            deletions = 3,
            isBinary = false,
        )

        assertEquals(1, diff.fromVersion)
        assertEquals(2, diff.toVersion)
        assertEquals(200, diff.sizeChange)
        assertFalse(diff.isBinary)
    }

    @Test
    fun testVersionDiffBinaryFile() {
        val diff = VersionDiff(
            fromVersion = 1,
            toVersion = 2,
            sizeChange = 500,
            additions = 0,
            deletions = 0,
            isBinary = true,
        )

        assertTrue(diff.isBinary)
        assertEquals(0, diff.additions)
        assertEquals(0, diff.deletions)
    }
}
