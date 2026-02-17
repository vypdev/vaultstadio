/**
 * VaultStadio Version Panel Tests
 */

package com.vaultstadio.app.ui.components.files

import com.vaultstadio.app.domain.model.FileVersion
import com.vaultstadio.app.domain.model.FileVersionHistory
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionPanelTest {

    @Test
    fun testVersionPanelWithEmptyHistory() {
        val history = FileVersionHistory(
            itemId = "item1",
            itemName = "document.txt",
            versions = emptyList(),
            totalVersions = 0,
            totalSize = 0,
        )

        assertEquals(0, history.totalVersions)
        assertEquals(0, history.totalSize)
        assertTrue(history.versions.isEmpty())
    }

    @Test
    fun testVersionPanelWithSingleVersion() {
        val now = Clock.System.now()
        val version = FileVersion(
            id = "v1",
            itemId = "item1",
            versionNumber = 1,
            size = 1024,
            checksum = "abc123",
            createdBy = "user1",
            createdAt = now,
            comment = null,
            isLatest = true,
            restoredFrom = null,
        )

        val history = FileVersionHistory(
            itemId = "item1",
            itemName = "document.txt",
            versions = listOf(version),
            totalVersions = 1,
            totalSize = 1024,
        )

        assertEquals(1, history.totalVersions)
        assertTrue(history.versions.first().isLatest)
    }

    @Test
    fun testVersionPanelWithMultipleVersions() {
        val now = Clock.System.now()
        val versions = listOf(
            FileVersion("v1", "item1", 1, 1000, "abc", "user1", now, null, false, null),
            FileVersion("v2", "item1", 2, 1100, "def", "user1", now, null, false, null),
            FileVersion("v3", "item1", 3, 1200, "ghi", "user1", now, null, true, null),
        )

        val history = FileVersionHistory(
            itemId = "item1",
            itemName = "document.txt",
            versions = versions,
            totalVersions = 3,
            totalSize = 3300,
        )

        assertEquals(3, history.totalVersions)
        assertEquals(3300, history.totalSize)

        // Only last should be latest
        assertFalse(versions[0].isLatest)
        assertFalse(versions[1].isLatest)
        assertTrue(versions[2].isLatest)
    }

    @Test
    fun testVersionPanelShowsOnlyLatest3() {
        val now = Clock.System.now()
        val versions = (1..10).map { i ->
            FileVersion(
                id = "v$i",
                itemId = "item1",
                versionNumber = i,
                size = 1000L * i,
                checksum = "hash$i",
                createdBy = "user1",
                createdAt = now,
                comment = null,
                isLatest = i == 10,
                restoredFrom = null,
            )
        }

        val history = FileVersionHistory(
            itemId = "item1",
            itemName = "big-file.txt",
            versions = versions,
            totalVersions = 10,
            totalSize = versions.sumOf { it.size },
        )

        // Panel should show take(3) versions
        val displayedVersions = history.versions.take(3)
        assertEquals(3, displayedVersions.size)
        assertEquals(10, history.totalVersions)
    }

    @Test
    fun testFileVersionIsRestore() {
        val now = Clock.System.now()
        val restoredVersion = FileVersion(
            id = "v5",
            itemId = "item1",
            versionNumber = 5,
            size = 1024,
            checksum = "abc123",
            createdBy = "user1",
            createdAt = now,
            comment = null,
            isLatest = true,
            restoredFrom = 2,
        )

        assertTrue(restoredVersion.isRestore)
        assertEquals(2, restoredVersion.restoredFrom)
    }

    @Test
    fun testFileVersionWithComment() {
        val now = Clock.System.now()
        val version = FileVersion(
            id = "v3",
            itemId = "item1",
            versionNumber = 3,
            size = 2048,
            checksum = "xyz789",
            createdBy = "user1",
            createdAt = now,
            comment = "Fixed the bug in line 42",
            isLatest = true,
            restoredFrom = null,
        )

        assertEquals("Fixed the bug in line 42", version.comment)
    }
}
