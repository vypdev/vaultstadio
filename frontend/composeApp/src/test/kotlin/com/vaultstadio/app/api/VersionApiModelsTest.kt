/**
 * VaultStadio Version API Models Tests
 */

package com.vaultstadio.app.api

import com.vaultstadio.app.domain.version.model.FileVersion
import com.vaultstadio.app.domain.version.model.FileVersionHistory
import com.vaultstadio.app.domain.version.model.RestoreVersionRequest
import com.vaultstadio.app.domain.version.model.VersionDiff
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VersionApiModelsTest {

    @Test
    fun testRestoreVersionRequestCreation() {
        val request = RestoreVersionRequest(
            versionNumber = 5,
            comment = "Restoring to stable version",
        )

        assertEquals(5, request.versionNumber)
        assertEquals("Restoring to stable version", request.comment)
    }

    @Test
    fun testRestoreVersionRequestWithoutComment() {
        val request = RestoreVersionRequest(
            versionNumber = 3,
        )

        assertEquals(3, request.versionNumber)
        assertNull(request.comment)
    }

    @Test
    fun testRestoreVersionRequestWithEmptyComment() {
        val request = RestoreVersionRequest(
            versionNumber = 7,
            comment = "",
        )

        assertEquals(7, request.versionNumber)
        assertEquals("", request.comment)
    }

    @Test
    fun testRestoreVersionRequestDifferentVersionNumbers() {
        val request1 = RestoreVersionRequest(versionNumber = 1)
        val request2 = RestoreVersionRequest(versionNumber = 100)
        val request3 = RestoreVersionRequest(versionNumber = 999)

        assertEquals(1, request1.versionNumber)
        assertEquals(100, request2.versionNumber)
        assertEquals(999, request3.versionNumber)
    }

    @Test
    fun testRestoreVersionRequestWithLongComment() {
        val longComment = "This is a very long comment that explains why we are restoring " +
            "to this particular version. The previous version had some bugs that " +
            "needed to be fixed, so we're rolling back to a known good state."

        val request = RestoreVersionRequest(
            versionNumber = 2,
            comment = longComment,
        )

        assertEquals(2, request.versionNumber)
        assertEquals(longComment, request.comment)
    }

    @Test
    fun testVersionDiffCreation() {
        val diff = VersionDiff(
            fromVersion = 1,
            toVersion = 2,
            sizeChange = 1024L,
            additions = 50,
            deletions = 10,
            isBinary = false,
        )

        assertEquals(1, diff.fromVersion)
        assertEquals(2, diff.toVersion)
        assertEquals(1024L, diff.sizeChange)
        assertEquals(50, diff.additions)
        assertEquals(10, diff.deletions)
        assertFalse(diff.isBinary)
    }

    @Test
    fun testVersionDiffBinary() {
        val diff = VersionDiff(
            fromVersion = 2,
            toVersion = 3,
            sizeChange = -500L,
            additions = 0,
            deletions = 0,
            isBinary = true,
        )

        assertTrue(diff.isBinary)
        assertEquals(-500L, diff.sizeChange)
    }

    @Test
    fun testFileVersionHistoryCreation() {
        val now = Instant.fromEpochMilliseconds(0L)
        val v1 = FileVersion(
            id = "v1",
            itemId = "item-1",
            versionNumber = 1,
            size = 1000L,
            checksum = "abc",
            createdBy = "user-1",
            createdAt = now,
            comment = null,
            isLatest = false,
            restoredFrom = null,
        )
        val history = FileVersionHistory(
            itemId = "item-1",
            itemName = "doc.pdf",
            versions = listOf(v1),
            totalVersions = 1,
            totalSize = 1000L,
        )
        assertEquals("item-1", history.itemId)
        assertEquals("doc.pdf", history.itemName)
        assertEquals(1, history.versions.size)
        assertEquals(1, history.totalVersions)
        assertEquals(1000L, history.totalSize)
    }

    @Test
    fun testFileVersionHistoryWithEmptyVersions() {
        val history = FileVersionHistory(
            itemId = "item-2",
            itemName = "empty.txt",
            versions = emptyList(),
            totalVersions = 0,
            totalSize = 0L,
        )
        assertEquals("item-2", history.itemId)
        assertEquals("empty.txt", history.itemName)
        assertTrue(history.versions.isEmpty())
        assertEquals(0, history.totalVersions)
        assertEquals(0L, history.totalSize)
    }

    @Test
    fun testFileVersionIsRestoreWhenRestoredFromSet() {
        val now = Instant.fromEpochMilliseconds(0L)
        val version = FileVersion(
            id = "v2",
            itemId = "item-1",
            versionNumber = 2,
            size = 1024L,
            checksum = "def",
            createdBy = "user-1",
            createdAt = now,
            comment = "Restored",
            isLatest = true,
            restoredFrom = 1,
        )
        assertTrue(version.isRestore)
        assertEquals(1, version.restoredFrom)
    }

    @Test
    fun testFileVersionIsNotRestoreWhenRestoredFromNull() {
        val now = Instant.fromEpochMilliseconds(0L)
        val version = FileVersion(
            id = "v1",
            itemId = "item-1",
            versionNumber = 1,
            size = 1000L,
            checksum = "abc",
            createdBy = "user-1",
            createdAt = now,
            comment = null,
            isLatest = true,
            restoredFrom = null,
        )
        assertFalse(version.isRestore)
        assertNull(version.restoredFrom)
    }
}
