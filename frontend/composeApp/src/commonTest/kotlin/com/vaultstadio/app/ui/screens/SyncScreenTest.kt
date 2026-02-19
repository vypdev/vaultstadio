/**
 * VaultStadio Sync Screen Tests
 */

package com.vaultstadio.app.ui.screens

import com.vaultstadio.app.domain.model.ConflictResolution
import com.vaultstadio.app.domain.model.ConflictType
import com.vaultstadio.app.domain.model.DeviceType
import com.vaultstadio.app.domain.model.SyncDevice
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncScreenTest {

    @Test
    fun testSyncDeviceModel() {
        val now = Clock.System.now()
        val device = SyncDevice(
            id = "d1",
            deviceId = "device-123",
            deviceName = "My MacBook",
            deviceType = DeviceType.DESKTOP_MAC,
            lastSyncAt = now,
            isActive = true,
            createdAt = now,
        )

        assertEquals("My MacBook", device.deviceName)
        assertEquals(DeviceType.DESKTOP_MAC, device.deviceType)
        assertTrue(device.isActive)
    }

    @Test
    fun testSyncDeviceInactive() {
        val now = Clock.System.now()
        val device = SyncDevice(
            id = "d2",
            deviceId = "device-456",
            deviceName = "Old Phone",
            deviceType = DeviceType.MOBILE_ANDROID,
            lastSyncAt = null,
            isActive = false,
            createdAt = now,
        )

        assertFalse(device.isActive)
        assertEquals(null, device.lastSyncAt)
    }

    @Test
    fun testDeviceTypeEnumValues() {
        val types = DeviceType.entries
        assertEquals(8, types.size)
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
    fun testConflictResolutionEnumValues() {
        val resolutions = ConflictResolution.entries
        assertEquals(5, resolutions.size)
        assertTrue(resolutions.contains(ConflictResolution.KEEP_LOCAL))
        assertTrue(resolutions.contains(ConflictResolution.KEEP_REMOTE))
        assertTrue(resolutions.contains(ConflictResolution.KEEP_BOTH))
        assertTrue(resolutions.contains(ConflictResolution.MERGE))
        assertTrue(resolutions.contains(ConflictResolution.MANUAL))
    }

    @Test
    fun testConflictTypeEnumValues() {
        val types = ConflictType.entries
        assertTrue(types.contains(ConflictType.EDIT_CONFLICT))
        assertTrue(types.contains(ConflictType.EDIT_DELETE))
        assertTrue(types.contains(ConflictType.DELETE_EDIT))
    }
}
