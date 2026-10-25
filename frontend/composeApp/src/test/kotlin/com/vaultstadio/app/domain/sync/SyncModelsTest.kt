/**
 * Unit tests for sync domain models: SyncDevice, DeviceType, ConflictType, ChangeType,
 * SyncChange, ConflictResolution.
 */

package com.vaultstadio.app.domain.sync

import com.vaultstadio.app.domain.sync.model.ChangeType
import com.vaultstadio.app.domain.sync.model.ConflictResolution
import com.vaultstadio.app.domain.sync.model.ConflictType
import com.vaultstadio.app.domain.sync.model.DeviceType
import com.vaultstadio.app.domain.sync.model.SyncChange
import com.vaultstadio.app.domain.sync.model.SyncDevice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.Instant

class SyncDeviceTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun syncDevice_construction() {
        val device = SyncDevice(
            id = "dev-1",
            deviceId = "device-uuid-123",
            deviceName = "My Laptop",
            deviceType = DeviceType.DESKTOP_MAC,
            lastSyncAt = testInstant,
            isActive = true,
            createdAt = testInstant,
        )
        assertEquals("dev-1", device.id)
        assertEquals("device-uuid-123", device.deviceId)
        assertEquals("My Laptop", device.deviceName)
        assertEquals(DeviceType.DESKTOP_MAC, device.deviceType)
        assertEquals(testInstant, device.lastSyncAt)
        assertTrue(device.isActive)
    }

    @Test
    fun syncDevice_defaultLastSyncAtNull() {
        val device = SyncDevice(
            id = "d2",
            deviceId = "id2",
            deviceName = "Phone",
            deviceType = DeviceType.MOBILE_ANDROID,
            isActive = false,
            createdAt = testInstant,
        )
        assertNull(device.lastSyncAt)
        assertEquals(DeviceType.MOBILE_ANDROID, device.deviceType)
    }
}

class DeviceTypeTest {

    @Test
    fun deviceType_hasExpectedValues() {
        val values = DeviceType.entries
        assertTrue(DeviceType.DESKTOP_WINDOWS in values)
        assertTrue(DeviceType.DESKTOP_MAC in values)
        assertTrue(DeviceType.DESKTOP_LINUX in values)
        assertTrue(DeviceType.MOBILE_ANDROID in values)
        assertTrue(DeviceType.MOBILE_IOS in values)
        assertTrue(DeviceType.WEB in values)
        assertTrue(DeviceType.CLI in values)
        assertTrue(DeviceType.OTHER in values)
        assertEquals(8, values.size)
    }

    @Test
    fun deviceType_names() {
        assertEquals("DESKTOP_MAC", DeviceType.DESKTOP_MAC.name)
        assertEquals("WEB", DeviceType.WEB.name)
    }
}

class ConflictTypeTest {

    @Test
    fun conflictType_hasExpectedValues() {
        val values = ConflictType.entries
        assertTrue(ConflictType.EDIT_CONFLICT in values)
        assertTrue(ConflictType.EDIT_DELETE in values)
        assertTrue(ConflictType.DELETE_EDIT in values)
        assertTrue(ConflictType.CREATE_CREATE in values)
        assertTrue(ConflictType.MOVE_MOVE in values)
        assertTrue(ConflictType.PARENT_DELETED in values)
        assertEquals(6, values.size)
    }

    @Test
    fun conflictType_names() {
        assertEquals("EDIT_CONFLICT", ConflictType.EDIT_CONFLICT.name)
        assertEquals("PARENT_DELETED", ConflictType.PARENT_DELETED.name)
    }
}

class ChangeTypeTest {

    @Test
    fun changeType_hasExpectedValues() {
        val values = ChangeType.entries
        assertTrue(ChangeType.CREATE in values)
        assertTrue(ChangeType.MODIFY in values)
        assertTrue(ChangeType.RENAME in values)
        assertTrue(ChangeType.MOVE in values)
        assertTrue(ChangeType.DELETE in values)
        assertTrue(ChangeType.RESTORE in values)
        assertTrue(ChangeType.TRASH in values)
        assertTrue(ChangeType.METADATA in values)
        assertEquals(8, values.size)
    }

    @Test
    fun changeType_names() {
        assertEquals("CREATE", ChangeType.CREATE.name)
        assertEquals("DELETE", ChangeType.DELETE.name)
    }
}

class SyncChangeTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun syncChange_construction() {
        val change = SyncChange(
            id = "ch-1",
            itemId = "item-1",
            changeType = ChangeType.MODIFY,
            timestamp = testInstant,
            cursor = 42L,
            oldPath = "/old",
            newPath = "/new",
            checksum = "abc123",
        )
        assertEquals("ch-1", change.id)
        assertEquals("item-1", change.itemId)
        assertEquals(ChangeType.MODIFY, change.changeType)
        assertEquals(42L, change.cursor)
        assertEquals("/old", change.oldPath)
        assertEquals("/new", change.newPath)
        assertEquals("abc123", change.checksum)
    }

    @Test
    fun syncChange_defaultOptionalNulls() {
        val change = SyncChange(
            id = "c2",
            itemId = "i2",
            changeType = ChangeType.CREATE,
            timestamp = testInstant,
            cursor = 1L,
        )
        assertNull(change.oldPath)
        assertNull(change.newPath)
        assertNull(change.checksum)
    }
}

class ConflictResolutionTest {

    @Test
    fun conflictResolution_hasExpectedValues() {
        val values = ConflictResolution.entries
        assertTrue(ConflictResolution.KEEP_LOCAL in values)
        assertTrue(ConflictResolution.KEEP_REMOTE in values)
        assertTrue(ConflictResolution.KEEP_BOTH in values)
        assertTrue(ConflictResolution.MERGE in values)
        assertTrue(ConflictResolution.MANUAL in values)
        assertEquals(5, values.size)
    }

    @Test
    fun conflictResolution_names() {
        assertEquals("KEEP_LOCAL", ConflictResolution.KEEP_LOCAL.name)
        assertEquals("MERGE", ConflictResolution.MERGE.name)
    }
}
