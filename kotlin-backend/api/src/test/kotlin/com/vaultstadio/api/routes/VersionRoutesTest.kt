/**
 * VaultStadio Version Routes Tests
 *
 * Unit tests for file versioning API data transfer objects.
 */

package com.vaultstadio.api.routes

import com.vaultstadio.api.routes.version.CleanupRequest
import com.vaultstadio.api.routes.version.CreateVersionRequest
import com.vaultstadio.api.routes.version.DiffPatchResponse
import com.vaultstadio.api.routes.version.FileVersionResponse
import com.vaultstadio.api.routes.version.RestoreVersionRequest
import com.vaultstadio.api.routes.version.VersionDiffResponse
import com.vaultstadio.api.routes.version.VersionHistoryResponse
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VersionRoutesTest {

    // ========================================================================
    // CreateVersionRequest Tests
    // ========================================================================

    @Test
    fun `CreateVersionRequest should allow comment`() {
        val request = CreateVersionRequest(comment = "Version comment")
        assertEquals("Version comment", request.comment)
    }

    @Test
    fun `CreateVersionRequest should allow null comment`() {
        val request = CreateVersionRequest()
        assertNull(request.comment)
    }

    @Test
    fun `CreateVersionRequest should have default null comment`() {
        val request = CreateVersionRequest(comment = null)
        assertNull(request.comment)
    }

    // ========================================================================
    // RestoreVersionRequest Tests
    // ========================================================================

    @Test
    fun `RestoreVersionRequest should store version number`() {
        val request = RestoreVersionRequest(versionNumber = 3)
        assertEquals(3, request.versionNumber)
    }

    @Test
    fun `RestoreVersionRequest should allow comment`() {
        val request = RestoreVersionRequest(
            versionNumber = 2,
            comment = "Restoring to fix bug",
        )

        assertEquals(2, request.versionNumber)
        assertEquals("Restoring to fix bug", request.comment)
    }

    @Test
    fun `RestoreVersionRequest should allow null comment`() {
        val request = RestoreVersionRequest(versionNumber = 1)
        assertNull(request.comment)
    }

    @Test
    fun `RestoreVersionRequest should support version 0`() {
        val request = RestoreVersionRequest(versionNumber = 0)
        assertEquals(0, request.versionNumber)
    }

    // ========================================================================
    // FileVersionResponse Tests
    // ========================================================================

    @Test
    fun `FileVersionResponse should store all properties`() {
        val response = FileVersionResponse(
            id = "version-123",
            itemId = "item-456",
            versionNumber = 5,
            size = 1024L,
            checksum = "md5checksum",
            createdBy = "user-789",
            createdAt = "2024-01-15T10:30:00Z",
            comment = "Updated file",
            isLatest = true,
            restoredFrom = null,
        )

        assertEquals("version-123", response.id)
        assertEquals("item-456", response.itemId)
        assertEquals(5, response.versionNumber)
        assertEquals(1024L, response.size)
        assertEquals("md5checksum", response.checksum)
        assertEquals("user-789", response.createdBy)
        assertEquals("2024-01-15T10:30:00Z", response.createdAt)
        assertEquals("Updated file", response.comment)
        assertTrue(response.isLatest)
        assertNull(response.restoredFrom)
    }

    @Test
    fun `FileVersionResponse should indicate not latest version`() {
        val response = FileVersionResponse(
            id = "version-1",
            itemId = "item-1",
            versionNumber = 1,
            size = 500L,
            checksum = "abc",
            createdBy = "user-1",
            createdAt = "2024-01-10T10:00:00Z",
            comment = null,
            isLatest = false,
            restoredFrom = null,
        )

        assertFalse(response.isLatest)
    }

    @Test
    fun `FileVersionResponse should indicate restore source`() {
        val response = FileVersionResponse(
            id = "version-123",
            itemId = "item-456",
            versionNumber = 6,
            size = 1024L,
            checksum = "md5checksum",
            createdBy = "user-789",
            createdAt = "2024-01-15T10:30:00Z",
            comment = "Restored from version 3",
            isLatest = true,
            restoredFrom = 3,
        )

        assertNotNull(response.restoredFrom)
        assertEquals(3, response.restoredFrom)
    }

    @Test
    fun `FileVersionResponse should allow null comment`() {
        val response = FileVersionResponse(
            id = "v1",
            itemId = "i1",
            versionNumber = 1,
            size = 100L,
            checksum = "abc",
            createdBy = "user",
            createdAt = "2024-01-01T00:00:00Z",
            comment = null,
            isLatest = true,
            restoredFrom = null,
        )

        assertNull(response.comment)
    }

    // ========================================================================
    // VersionHistoryResponse Tests
    // ========================================================================

    @Test
    fun `VersionHistoryResponse should store all properties`() {
        val versions = listOf(
            FileVersionResponse(
                id = "v1",
                itemId = "item-123",
                versionNumber = 1,
                size = 500L,
                checksum = "abc",
                createdBy = "user-1",
                createdAt = "2024-01-10T10:00:00Z",
                comment = null,
                isLatest = false,
                restoredFrom = null,
            ),
            FileVersionResponse(
                id = "v2",
                itemId = "item-123",
                versionNumber = 2,
                size = 600L,
                checksum = "def",
                createdBy = "user-1",
                createdAt = "2024-01-15T10:00:00Z",
                comment = "Updated",
                isLatest = true,
                restoredFrom = null,
            ),
        )

        val response = VersionHistoryResponse(
            itemId = "item-123",
            itemName = "document.pdf",
            versions = versions,
            totalVersions = 2,
            totalSize = 1100L,
        )

        assertEquals("item-123", response.itemId)
        assertEquals("document.pdf", response.itemName)
        assertEquals(2, response.versions.size)
        assertEquals(2, response.totalVersions)
        assertEquals(1100L, response.totalSize)
    }

    @Test
    fun `VersionHistoryResponse should allow empty versions list`() {
        val response = VersionHistoryResponse(
            itemId = "item-123",
            itemName = "empty.txt",
            versions = emptyList(),
            totalVersions = 0,
            totalSize = 0L,
        )

        assertTrue(response.versions.isEmpty())
        assertEquals(0, response.totalVersions)
        assertEquals(0L, response.totalSize)
    }

    @Test
    fun `VersionHistoryResponse versions should be in order`() {
        val versions = listOf(
            FileVersionResponse("v1", "item", 1, 100L, "a", "user", "2024-01-01", null, false, null),
            FileVersionResponse("v2", "item", 2, 200L, "b", "user", "2024-01-02", null, false, null),
            FileVersionResponse("v3", "item", 3, 300L, "c", "user", "2024-01-03", null, true, null),
        )

        val response = VersionHistoryResponse(
            itemId = "item",
            itemName = "file.txt",
            versions = versions,
            totalVersions = 3,
            totalSize = 600L,
        )

        assertEquals(1, response.versions[0].versionNumber)
        assertEquals(2, response.versions[1].versionNumber)
        assertEquals(3, response.versions[2].versionNumber)
    }

    // ========================================================================
    // VersionDiffResponse Tests
    // ========================================================================

    @Test
    fun `VersionDiffResponse should store all properties`() {
        val patches = listOf(
            DiffPatchResponse(
                operation = "ADD",
                startLine = 15,
                endLine = 20,
                oldContent = null,
                newContent = "new content here",
            ),
            DiffPatchResponse(
                operation = "DELETE",
                startLine = 30,
                endLine = 35,
                oldContent = "old content removed",
                newContent = null,
            ),
        )

        val response = VersionDiffResponse(
            fromVersion = 1,
            toVersion = 3,
            sizeChange = 256L,
            additions = 10,
            deletions = 5,
            isBinary = false,
            patches = patches,
        )

        assertEquals(1, response.fromVersion)
        assertEquals(3, response.toVersion)
        assertEquals(256L, response.sizeChange)
        assertEquals(10, response.additions)
        assertEquals(5, response.deletions)
        assertFalse(response.isBinary)
        assertEquals(2, response.patches.size)
    }

    @Test
    fun `VersionDiffResponse should indicate binary file`() {
        val response = VersionDiffResponse(
            fromVersion = 1,
            toVersion = 2,
            sizeChange = 1000L,
            additions = 0,
            deletions = 0,
            isBinary = true,
            patches = emptyList(),
        )

        assertTrue(response.isBinary)
        assertTrue(response.patches.isEmpty())
    }

    @Test
    fun `VersionDiffResponse should allow negative size change`() {
        val response = VersionDiffResponse(
            fromVersion = 2,
            toVersion = 3,
            sizeChange = -500L,
            additions = 0,
            deletions = 10,
            isBinary = false,
            patches = emptyList(),
        )

        assertEquals(-500L, response.sizeChange)
    }

    // ========================================================================
    // DiffPatchResponse Tests
    // ========================================================================

    @Test
    fun `DiffPatchResponse should represent ADD operation`() {
        val patch = DiffPatchResponse(
            operation = "ADD",
            startLine = 10,
            endLine = 15,
            oldContent = null,
            newContent = "added lines",
        )

        assertEquals("ADD", patch.operation)
        assertNull(patch.oldContent)
        assertNotNull(patch.newContent)
    }

    @Test
    fun `DiffPatchResponse should represent DELETE operation`() {
        val patch = DiffPatchResponse(
            operation = "DELETE",
            startLine = 20,
            endLine = 25,
            oldContent = "deleted lines",
            newContent = null,
        )

        assertEquals("DELETE", patch.operation)
        assertNotNull(patch.oldContent)
        assertNull(patch.newContent)
    }

    @Test
    fun `DiffPatchResponse should represent MODIFY operation`() {
        val patch = DiffPatchResponse(
            operation = "MODIFY",
            startLine = 30,
            endLine = 35,
            oldContent = "old version",
            newContent = "new version",
        )

        assertEquals("MODIFY", patch.operation)
        assertNotNull(patch.oldContent)
        assertNotNull(patch.newContent)
    }

    // ========================================================================
    // CleanupRequest Tests
    // ========================================================================

    @Test
    fun `CleanupRequest should have default values`() {
        val request = CleanupRequest()

        assertNull(request.maxVersions)
        assertNull(request.maxAgeDays)
        assertEquals(1, request.minVersionsToKeep)
    }

    @Test
    fun `CleanupRequest should accept maxVersions`() {
        val request = CleanupRequest(maxVersions = 10)

        assertEquals(10, request.maxVersions)
        assertNull(request.maxAgeDays)
        assertEquals(1, request.minVersionsToKeep)
    }

    @Test
    fun `CleanupRequest should accept maxAgeDays`() {
        val request = CleanupRequest(maxAgeDays = 90)

        assertNull(request.maxVersions)
        assertEquals(90, request.maxAgeDays)
        assertEquals(1, request.minVersionsToKeep)
    }

    @Test
    fun `CleanupRequest should accept all parameters`() {
        val request = CleanupRequest(
            maxVersions = 10,
            maxAgeDays = 90,
            minVersionsToKeep = 3,
        )

        assertEquals(10, request.maxVersions)
        assertEquals(90, request.maxAgeDays)
        assertEquals(3, request.minVersionsToKeep)
    }

    @Test
    fun `CleanupRequest should allow zero minVersionsToKeep`() {
        val request = CleanupRequest(minVersionsToKeep = 0)
        assertEquals(0, request.minVersionsToKeep)
    }

    @Test
    fun `CleanupRequest should allow very large maxVersions`() {
        val request = CleanupRequest(maxVersions = 1000000)
        assertEquals(1000000, request.maxVersions)
    }
}
