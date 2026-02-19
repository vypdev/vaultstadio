/**
 * VaultStadio Sync Model Tests
 */

package com.vaultstadio.core.domain.model

import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncTest {

    @Test
    fun `SyncDevice should have correct properties`() {
        val now = Clock.System.now()
        val device = SyncDevice(
            id = "device-1",
            userId = "user-1",
            deviceId = "my-laptop-123",
            deviceName = "My Laptop",
            deviceType = DeviceType.DESKTOP_MAC,
            lastSyncAt = now,
            lastSyncCursor = "12345",
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )

        assertEquals("device-1", device.id)
        assertEquals("user-1", device.userId)
        assertEquals("my-laptop-123", device.deviceId)
        assertEquals("My Laptop", device.deviceName)
        assertEquals(DeviceType.DESKTOP_MAC, device.deviceType)
        assertTrue(device.isActive)
    }

    @Test
    fun `DeviceType should have all platform types`() {
        val types = DeviceType.entries

        assertTrue(types.contains(DeviceType.DESKTOP_WINDOWS))
        assertTrue(types.contains(DeviceType.DESKTOP_MAC))
        assertTrue(types.contains(DeviceType.DESKTOP_LINUX))
        assertTrue(types.contains(DeviceType.MOBILE_ANDROID))
        assertTrue(types.contains(DeviceType.MOBILE_IOS))
        assertTrue(types.contains(DeviceType.WEB))
        assertTrue(types.contains(DeviceType.CLI))
        assertTrue(types.contains(DeviceType.OTHER))
    }

    @Test
    fun `SyncChange should represent a file change`() {
        val change = SyncChange(
            id = "change-1",
            itemId = "item-1",
            changeType = ChangeType.MODIFY,
            userId = "user-1",
            deviceId = "device-1",
            timestamp = Clock.System.now(),
            cursor = 100,
            oldPath = "/old/path.txt",
            newPath = "/new/path.txt",
            checksum = "abc123",
        )

        assertEquals("change-1", change.id)
        assertEquals(ChangeType.MODIFY, change.changeType)
        assertEquals(100, change.cursor)
    }

    @Test
    fun `ChangeType should have all change types`() {
        val types = ChangeType.entries

        assertTrue(types.contains(ChangeType.CREATE))
        assertTrue(types.contains(ChangeType.MODIFY))
        assertTrue(types.contains(ChangeType.RENAME))
        assertTrue(types.contains(ChangeType.MOVE))
        assertTrue(types.contains(ChangeType.DELETE))
        assertTrue(types.contains(ChangeType.RESTORE))
        assertTrue(types.contains(ChangeType.TRASH))
        assertTrue(types.contains(ChangeType.METADATA))
    }

    @Test
    fun `SyncConflict should be pending when not resolved`() {
        val now = Clock.System.now()
        val localChange = SyncChange(
            id = "local-1",
            itemId = "item-1",
            changeType = ChangeType.MODIFY,
            userId = "user-1",
            timestamp = now,
            cursor = 100,
        )
        val remoteChange = SyncChange(
            id = "remote-1",
            itemId = "item-1",
            changeType = ChangeType.MODIFY,
            userId = "user-1",
            timestamp = now,
            cursor = 101,
        )

        val conflict = SyncConflict(
            id = "conflict-1",
            itemId = "item-1",
            localChange = localChange,
            remoteChange = remoteChange,
            conflictType = ConflictType.EDIT_CONFLICT,
            createdAt = now,
        )

        assertTrue(conflict.isPending)
        assertNull(conflict.resolvedAt)
        assertEquals(ConflictType.EDIT_CONFLICT, conflict.conflictType)
    }

    @Test
    fun `ConflictType should have all conflict types`() {
        val types = ConflictType.entries

        assertTrue(types.contains(ConflictType.EDIT_CONFLICT))
        assertTrue(types.contains(ConflictType.EDIT_DELETE))
        assertTrue(types.contains(ConflictType.DELETE_EDIT))
        assertTrue(types.contains(ConflictType.CREATE_CREATE))
        assertTrue(types.contains(ConflictType.MOVE_MOVE))
        assertTrue(types.contains(ConflictType.PARENT_DELETED))
    }

    @Test
    fun `ConflictResolution should have all resolution types`() {
        val resolutions = ConflictResolution.entries

        assertTrue(resolutions.contains(ConflictResolution.KEEP_LOCAL))
        assertTrue(resolutions.contains(ConflictResolution.KEEP_REMOTE))
        assertTrue(resolutions.contains(ConflictResolution.KEEP_BOTH))
        assertTrue(resolutions.contains(ConflictResolution.MERGE))
        assertTrue(resolutions.contains(ConflictResolution.MANUAL))
    }

    @Test
    fun `SyncRequest should have default values`() {
        val request = SyncRequest(deviceId = "device-1")

        assertEquals("device-1", request.deviceId)
        assertNull(request.cursor)
        assertEquals(1000, request.limit)
        assertTrue(request.includeDeleted)
    }

    @Test
    fun `FileSignature should have block checksums`() {
        val signature = FileSignature(
            itemId = "item-1",
            versionNumber = 1,
            blockSize = 4096,
            blocks = listOf(
                BlockChecksum(0, 12345L, "md5hash1"),
                BlockChecksum(1, 67890L, "md5hash2"),
            ),
        )

        assertEquals("item-1", signature.itemId)
        assertEquals(1, signature.versionNumber)
        assertEquals(4096, signature.blockSize)
        assertEquals(2, signature.blocks.size)
    }
}
