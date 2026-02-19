/**
 * VaultStadio Sync Routes Tests
 *
 * Unit tests for sync API data transfer objects.
 */

package com.vaultstadio.api.routes

import com.vaultstadio.api.routes.sync.BlockSignatureResponse
import com.vaultstadio.api.routes.sync.ClientChange
import com.vaultstadio.api.routes.sync.DeltaBlock
import com.vaultstadio.api.routes.sync.DeltaUploadRequest
import com.vaultstadio.api.routes.sync.DeltaUploadResponse
import com.vaultstadio.api.routes.sync.FileSignatureResponse
import com.vaultstadio.api.routes.sync.PushChangesRequest
import com.vaultstadio.api.routes.sync.RegisterDeviceRequest
import com.vaultstadio.api.routes.sync.ResolveConflictRequest
import com.vaultstadio.api.routes.sync.SyncChangeResponse
import com.vaultstadio.api.routes.sync.SyncDeviceResponse
import com.vaultstadio.api.routes.sync.SyncRequest
import com.vaultstadio.api.routes.sync.SyncResponse
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncRoutesTest {

    // ========================================================================
    // RegisterDeviceRequest Tests
    // ========================================================================

    @Test
    fun `RegisterDeviceRequest should store all properties`() {
        val request = RegisterDeviceRequest(
            deviceId = "device-123",
            deviceName = "My Phone",
            deviceType = "MOBILE",
        )

        assertEquals("device-123", request.deviceId)
        assertEquals("My Phone", request.deviceName)
        assertEquals("MOBILE", request.deviceType)
    }

    @Test
    fun `RegisterDeviceRequest should support different device types`() {
        val desktop = RegisterDeviceRequest("d1", "Desktop", "DESKTOP")
        val mobile = RegisterDeviceRequest("d2", "Mobile", "MOBILE")
        val tablet = RegisterDeviceRequest("d3", "Tablet", "TABLET")
        val web = RegisterDeviceRequest("d4", "Web", "WEB")
        val other = RegisterDeviceRequest("d5", "Other", "OTHER")

        assertEquals("DESKTOP", desktop.deviceType)
        assertEquals("MOBILE", mobile.deviceType)
        assertEquals("TABLET", tablet.deviceType)
        assertEquals("WEB", web.deviceType)
        assertEquals("OTHER", other.deviceType)
    }

    // ========================================================================
    // SyncDeviceResponse Tests
    // ========================================================================

    @Test
    fun `SyncDeviceResponse should store all properties`() {
        val response = SyncDeviceResponse(
            id = "id-123",
            deviceId = "device-456",
            deviceName = "Work Laptop",
            deviceType = "DESKTOP",
            lastSyncAt = "2024-01-15T10:30:00Z",
            isActive = true,
            createdAt = "2024-01-01T00:00:00Z",
        )

        assertEquals("id-123", response.id)
        assertEquals("device-456", response.deviceId)
        assertEquals("Work Laptop", response.deviceName)
        assertEquals("DESKTOP", response.deviceType)
        assertEquals("2024-01-15T10:30:00Z", response.lastSyncAt)
        assertTrue(response.isActive)
        assertEquals("2024-01-01T00:00:00Z", response.createdAt)
    }

    @Test
    fun `SyncDeviceResponse should allow null lastSyncAt`() {
        val response = SyncDeviceResponse(
            id = "id-123",
            deviceId = "device-456",
            deviceName = "New Device",
            deviceType = "MOBILE",
            lastSyncAt = null,
            isActive = true,
            createdAt = "2024-01-01T00:00:00Z",
        )

        assertNull(response.lastSyncAt)
    }

    // ========================================================================
    // SyncRequest Tests
    // ========================================================================

    @Test
    fun `SyncRequest should have default values`() {
        val request = SyncRequest()

        assertNull(request.cursor)
        assertEquals(1000, request.limit)
        assertTrue(request.includeDeleted)
    }

    @Test
    fun `SyncRequest should accept custom values`() {
        val request = SyncRequest(
            cursor = "12345",
            limit = 500,
            includeDeleted = false,
        )

        assertEquals("12345", request.cursor)
        assertEquals(500, request.limit)
        assertEquals(false, request.includeDeleted)
    }

    @Test
    fun `SyncRequest should allow zero limit`() {
        val request = SyncRequest(limit = 0)
        assertEquals(0, request.limit)
    }

    // ========================================================================
    // SyncChangeResponse Tests
    // ========================================================================

    @Test
    fun `SyncChangeResponse should store all properties`() {
        val response = SyncChangeResponse(
            id = "change-123",
            itemId = "item-456",
            changeType = "MODIFY",
            timestamp = "2024-01-15T10:30:00Z",
            cursor = 12345L,
            oldPath = "/old/path",
            newPath = "/new/path",
            checksum = "abc123",
        )

        assertEquals("change-123", response.id)
        assertEquals("item-456", response.itemId)
        assertEquals("MODIFY", response.changeType)
        assertEquals("2024-01-15T10:30:00Z", response.timestamp)
        assertEquals(12345L, response.cursor)
        assertEquals("/old/path", response.oldPath)
        assertEquals("/new/path", response.newPath)
        assertEquals("abc123", response.checksum)
    }

    @Test
    fun `SyncChangeResponse should allow null optional fields`() {
        val response = SyncChangeResponse(
            id = "change-123",
            itemId = "item-456",
            changeType = "CREATE",
            timestamp = "2024-01-15T10:30:00Z",
            cursor = 1L,
            oldPath = null,
            newPath = null,
            checksum = null,
        )

        assertNull(response.oldPath)
        assertNull(response.newPath)
        assertNull(response.checksum)
    }

    // ========================================================================
    // SyncResponse Tests
    // ========================================================================

    @Test
    fun `SyncResponse should store all properties`() {
        val response = SyncResponse(
            changes = emptyList(),
            cursor = "67890",
            hasMore = false,
            conflicts = emptyList(),
            serverTime = "2024-01-15T10:30:00Z",
        )

        assertTrue(response.changes.isEmpty())
        assertEquals("67890", response.cursor)
        assertEquals(false, response.hasMore)
        assertTrue(response.conflicts.isEmpty())
        assertEquals("2024-01-15T10:30:00Z", response.serverTime)
    }

    @Test
    fun `SyncResponse should indicate more changes available`() {
        val response = SyncResponse(
            changes = emptyList(),
            cursor = "100",
            hasMore = true,
            conflicts = emptyList(),
            serverTime = "2024-01-15T10:30:00Z",
        )

        assertTrue(response.hasMore)
    }

    // ========================================================================
    // ClientChange Tests
    // ========================================================================

    @Test
    fun `ClientChange should have default values for optional fields`() {
        val change = ClientChange(
            itemId = "item-123",
            changeType = "MODIFY",
        )

        assertEquals("item-123", change.itemId)
        assertEquals("MODIFY", change.changeType)
        assertNull(change.oldPath)
        assertNull(change.newPath)
        assertNull(change.checksum)
        assertTrue(change.metadata.isEmpty())
    }

    @Test
    fun `ClientChange should accept all optional fields`() {
        val change = ClientChange(
            itemId = "item-123",
            changeType = "MOVE",
            oldPath = "/old/location",
            newPath = "/new/location",
            checksum = "newchecksum",
            metadata = mapOf("key1" to "value1", "key2" to "value2"),
        )

        assertEquals("/old/location", change.oldPath)
        assertEquals("/new/location", change.newPath)
        assertEquals("newchecksum", change.checksum)
        assertEquals(2, change.metadata.size)
        assertEquals("value1", change.metadata["key1"])
    }

    // ========================================================================
    // PushChangesRequest Tests
    // ========================================================================

    @Test
    fun `PushChangesRequest should store list of changes`() {
        val changes = listOf(
            ClientChange(itemId = "item-1", changeType = "CREATE"),
            ClientChange(itemId = "item-2", changeType = "MODIFY"),
            ClientChange(itemId = "item-3", changeType = "DELETE"),
        )

        val request = PushChangesRequest(changes = changes)

        assertEquals(3, request.changes.size)
        assertEquals("item-1", request.changes[0].itemId)
        assertEquals("CREATE", request.changes[0].changeType)
    }

    @Test
    fun `PushChangesRequest should allow empty changes list`() {
        val request = PushChangesRequest(changes = emptyList())
        assertTrue(request.changes.isEmpty())
    }

    // ========================================================================
    // ResolveConflictRequest Tests
    // ========================================================================

    @Test
    fun `ResolveConflictRequest should accept KEEP_LOCAL resolution`() {
        val request = ResolveConflictRequest(resolution = "KEEP_LOCAL")
        assertEquals("KEEP_LOCAL", request.resolution)
    }

    @Test
    fun `ResolveConflictRequest should accept KEEP_REMOTE resolution`() {
        val request = ResolveConflictRequest(resolution = "KEEP_REMOTE")
        assertEquals("KEEP_REMOTE", request.resolution)
    }

    @Test
    fun `ResolveConflictRequest should accept KEEP_BOTH resolution`() {
        val request = ResolveConflictRequest(resolution = "KEEP_BOTH")
        assertEquals("KEEP_BOTH", request.resolution)
    }

    @Test
    fun `ResolveConflictRequest should accept MERGE resolution`() {
        val request = ResolveConflictRequest(resolution = "MERGE")
        assertEquals("MERGE", request.resolution)
    }

    // ========================================================================
    // FileSignatureResponse Tests
    // ========================================================================

    @Test
    fun `FileSignatureResponse should store all properties`() {
        val response = FileSignatureResponse(
            itemId = "item-123",
            blockSize = 4096,
            versionNumber = 3,
            blocks = listOf(
                BlockSignatureResponse(index = 0, weakChecksum = 12345L, strongChecksum = "abc"),
                BlockSignatureResponse(index = 1, weakChecksum = 67890L, strongChecksum = "def"),
            ),
        )

        assertEquals("item-123", response.itemId)
        assertEquals(4096, response.blockSize)
        assertEquals(3, response.versionNumber)
        assertEquals(2, response.blocks.size)
    }

    @Test
    fun `FileSignatureResponse should allow empty blocks list`() {
        val response = FileSignatureResponse(
            itemId = "item-123",
            blockSize = 8192,
            versionNumber = 1,
            blocks = emptyList(),
        )

        assertTrue(response.blocks.isEmpty())
    }

    // ========================================================================
    // BlockSignatureResponse Tests
    // ========================================================================

    @Test
    fun `BlockSignatureResponse should store all properties`() {
        val response = BlockSignatureResponse(
            index = 5,
            weakChecksum = 123456789L,
            strongChecksum = "md5hash123456",
        )

        assertEquals(5, response.index)
        assertEquals(123456789L, response.weakChecksum)
        assertEquals("md5hash123456", response.strongChecksum)
    }

    // ========================================================================
    // DeltaUploadRequest Tests
    // ========================================================================

    @Test
    fun `DeltaUploadRequest should store all properties`() {
        val request = DeltaUploadRequest(
            baseVersion = 5,
            blocks = listOf(
                DeltaBlock(index = 0, operation = "COPY", sourceIndex = 0),
                DeltaBlock(index = 1, operation = "INSERT", data = "YmFzZTY0ZGF0YQ=="),
            ),
            newChecksum = "md5hash123",
        )

        assertEquals(5, request.baseVersion)
        assertEquals(2, request.blocks.size)
        assertEquals("md5hash123", request.newChecksum)
    }

    @Test
    fun `DeltaUploadRequest should allow empty blocks list`() {
        val request = DeltaUploadRequest(
            baseVersion = 1,
            blocks = emptyList(),
            newChecksum = "empty",
        )

        assertTrue(request.blocks.isEmpty())
    }

    // ========================================================================
    // DeltaBlock Tests
    // ========================================================================

    @Test
    fun `DeltaBlock COPY should have sourceIndex`() {
        val block = DeltaBlock(
            index = 0,
            operation = "COPY",
            sourceIndex = 5,
            data = null,
        )

        assertEquals("COPY", block.operation)
        assertEquals(5, block.sourceIndex)
        assertNull(block.data)
    }

    @Test
    fun `DeltaBlock INSERT should have data`() {
        val block = DeltaBlock(
            index = 1,
            operation = "INSERT",
            sourceIndex = null,
            data = "base64encodeddata",
        )

        assertEquals("INSERT", block.operation)
        assertNull(block.sourceIndex)
        assertEquals("base64encodeddata", block.data)
    }

    // ========================================================================
    // DeltaUploadResponse Tests
    // ========================================================================

    @Test
    fun `DeltaUploadResponse should store all properties`() {
        val response = DeltaUploadResponse(
            success = true,
            itemId = "item-123",
            appliedBlocks = 5,
            newVersion = 4,
            newChecksum = "newmd5hash",
        )

        assertTrue(response.success)
        assertEquals("item-123", response.itemId)
        assertEquals(5, response.appliedBlocks)
        assertEquals(4, response.newVersion)
        assertEquals("newmd5hash", response.newChecksum)
    }

    @Test
    fun `DeltaUploadResponse should support failure case`() {
        val response = DeltaUploadResponse(
            success = false,
            itemId = "item-123",
            appliedBlocks = 0,
            newVersion = 0,
            newChecksum = "",
        )

        assertEquals(false, response.success)
        assertEquals(0, response.appliedBlocks)
    }
}
