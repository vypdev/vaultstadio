/**
 * VaultStadio Sync API Models Tests
 */

package com.vaultstadio.app.api

import com.vaultstadio.app.domain.sync.model.ConflictResolution
import com.vaultstadio.app.domain.sync.model.DeviceType
import com.vaultstadio.app.domain.sync.model.SyncRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncApiModelsTest {

    @Test
    fun testSyncRequestCreation() {
        val request = SyncRequest(
            cursor = "abc123",
            limit = 500,
            includeDeleted = false,
        )

        assertEquals("abc123", request.cursor)
        assertEquals(500, request.limit)
        assertFalse(request.includeDeleted)
    }

    @Test
    fun testSyncRequestDefaults() {
        val request = SyncRequest()

        assertEquals(null, request.cursor)
        assertEquals(1000, request.limit)
        assertTrue(request.includeDeleted)
    }

    @Test
    fun testSyncRequestWithCursor() {
        val request = SyncRequest(cursor = "next-page-token")

        assertEquals("next-page-token", request.cursor)
        assertEquals(1000, request.limit)
    }

    @Test
    fun testDeviceTypeDesktopValues() {
        val desktopTypes = listOf(
            DeviceType.DESKTOP_WINDOWS,
            DeviceType.DESKTOP_MAC,
            DeviceType.DESKTOP_LINUX,
        )

        desktopTypes.forEach { type ->
            assertTrue(type.name.contains("DESKTOP"))
        }
    }

    @Test
    fun testDeviceTypeMobileValues() {
        val mobileTypes = listOf(
            DeviceType.MOBILE_ANDROID,
            DeviceType.MOBILE_IOS,
        )

        mobileTypes.forEach { type ->
            assertTrue(type.name.contains("MOBILE"))
        }
    }

    @Test
    fun testDeviceTypeOtherValues() {
        assertTrue(DeviceType.entries.contains(DeviceType.WEB))
        assertTrue(DeviceType.entries.contains(DeviceType.CLI))
        assertTrue(DeviceType.entries.contains(DeviceType.OTHER))
    }

    @Test
    fun testConflictResolutionKeepLocal() {
        val resolution = ConflictResolution.KEEP_LOCAL
        assertEquals("KEEP_LOCAL", resolution.name)
    }

    @Test
    fun testConflictResolutionKeepRemote() {
        val resolution = ConflictResolution.KEEP_REMOTE
        assertEquals("KEEP_REMOTE", resolution.name)
    }

    @Test
    fun testConflictResolutionKeepBoth() {
        val resolution = ConflictResolution.KEEP_BOTH
        assertEquals("KEEP_BOTH", resolution.name)
    }

    @Test
    fun testConflictResolutionMerge() {
        val resolution = ConflictResolution.MERGE
        assertEquals("MERGE", resolution.name)
    }

    @Test
    fun testConflictResolutionManual() {
        val resolution = ConflictResolution.MANUAL
        assertEquals("MANUAL", resolution.name)
    }

    @Test
    fun testConflictResolutionValues() {
        val resolutions = ConflictResolution.entries
        assertEquals(5, resolutions.size)
    }
}
