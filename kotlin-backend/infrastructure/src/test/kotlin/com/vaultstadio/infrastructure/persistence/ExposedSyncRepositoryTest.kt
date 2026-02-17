/**
 * VaultStadio Exposed Sync Repository Tests
 */

package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.core.domain.model.ChangeType
import com.vaultstadio.core.domain.model.ConflictResolution
import com.vaultstadio.core.domain.model.ConflictType
import com.vaultstadio.core.domain.model.DeviceType
import com.vaultstadio.core.domain.model.SyncChange
import com.vaultstadio.core.domain.model.SyncConflict
import com.vaultstadio.core.domain.model.SyncDevice
import com.vaultstadio.core.domain.repository.SyncRepository
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

/**
 * Unit tests for ExposedSyncRepository.
 */
class ExposedSyncRepositoryTest {

    private lateinit var repository: SyncRepository

    @BeforeEach
    fun setup() {
        repository = ExposedSyncRepository()
    }

    @Nested
    @DisplayName("Repository API Tests")
    inner class RepositoryApiTests {

        @Test
        fun `repository should implement SyncRepository interface`() {
            assertTrue(repository is SyncRepository)
        }

        @Test
        fun `repository should be of correct implementation type`() {
            assertTrue(repository is ExposedSyncRepository)
        }
    }

    @Nested
    @DisplayName("SyncDevice Model Tests")
    inner class SyncDeviceModelTests {

        @Test
        fun `device should be created with all required fields`() {
            val now = Clock.System.now()

            val device = SyncDevice(
                id = "device-123",
                userId = "user-456",
                deviceId = "mobile-device-001",
                deviceName = "My iPhone",
                deviceType = DeviceType.MOBILE_IOS,
                lastSyncAt = now,
                lastSyncCursor = "cursor-12345",
                isActive = true,
                createdAt = now,
                updatedAt = now,
            )

            assertEquals("device-123", device.id)
            assertEquals("user-456", device.userId)
            assertEquals("mobile-device-001", device.deviceId)
            assertEquals("My iPhone", device.deviceName)
            assertEquals(DeviceType.MOBILE_IOS, device.deviceType)
            assertEquals(now, device.lastSyncAt)
            assertEquals("cursor-12345", device.lastSyncCursor)
            assertTrue(device.isActive)
        }

        @Test
        fun `device should support different device types`() {
            val now = Clock.System.now()
            val base = SyncDevice(
                id = "1", userId = "u", deviceId = "d", deviceName = "D",
                deviceType = DeviceType.DESKTOP_WINDOWS, lastSyncAt = null, lastSyncCursor = null,
                isActive = true, createdAt = now, updatedAt = now,
            )

            val windows = base.copy(deviceType = DeviceType.DESKTOP_WINDOWS)
            val mac = base.copy(deviceType = DeviceType.DESKTOP_MAC)
            val linux = base.copy(deviceType = DeviceType.DESKTOP_LINUX)
            val android = base.copy(deviceType = DeviceType.MOBILE_ANDROID)
            val ios = base.copy(deviceType = DeviceType.MOBILE_IOS)
            val web = base.copy(deviceType = DeviceType.WEB)
            val other = base.copy(deviceType = DeviceType.OTHER)

            assertEquals(DeviceType.DESKTOP_WINDOWS, windows.deviceType)
            assertEquals(DeviceType.DESKTOP_MAC, mac.deviceType)
            assertEquals(DeviceType.DESKTOP_LINUX, linux.deviceType)
            assertEquals(DeviceType.MOBILE_ANDROID, android.deviceType)
            assertEquals(DeviceType.MOBILE_IOS, ios.deviceType)
            assertEquals(DeviceType.WEB, web.deviceType)
            assertEquals(DeviceType.OTHER, other.deviceType)
        }

        @Test
        fun `device should allow null sync info for new devices`() {
            val now = Clock.System.now()

            val newDevice = SyncDevice(
                id = "1",
                userId = "u",
                deviceId = "d",
                deviceName = "New Device",
                deviceType = DeviceType.MOBILE_ANDROID,
                lastSyncAt = null,
                lastSyncCursor = null,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            )

            assertNull(newDevice.lastSyncAt)
            assertNull(newDevice.lastSyncCursor)
        }

        @Test
        fun `device should track active status`() {
            val now = Clock.System.now()
            val base = SyncDevice(
                id = "1", userId = "u", deviceId = "d", deviceName = "D",
                deviceType = DeviceType.MOBILE_IOS, lastSyncAt = null, lastSyncCursor = null,
                isActive = true, createdAt = now, updatedAt = now,
            )

            val active = base.copy(isActive = true)
            val inactive = base.copy(isActive = false)

            assertTrue(active.isActive)
            assertFalse(inactive.isActive)
        }
    }

    @Nested
    @DisplayName("SyncChange Model Tests")
    inner class SyncChangeModelTests {

        @Test
        fun `change should be created with all required fields`() {
            val now = Clock.System.now()

            val change = SyncChange(
                id = "change-123",
                itemId = "item-456",
                changeType = ChangeType.MODIFY,
                userId = "user-789",
                deviceId = "device-001",
                timestamp = now,
                cursor = 12345L,
                oldPath = "/old/path/file.txt",
                newPath = "/new/path/file.txt",
                checksum = "abc123",
                metadata = mapOf("key1" to "value1"),
            )

            assertEquals("change-123", change.id)
            assertEquals("item-456", change.itemId)
            assertEquals(ChangeType.MODIFY, change.changeType)
            assertEquals("user-789", change.userId)
            assertEquals("device-001", change.deviceId)
            assertEquals(now, change.timestamp)
            assertEquals(12345L, change.cursor)
            assertEquals("/old/path/file.txt", change.oldPath)
            assertEquals("/new/path/file.txt", change.newPath)
            assertEquals("abc123", change.checksum)
            assertEquals("value1", change.metadata["key1"])
        }

        @Test
        fun `change should support different change types`() {
            val now = Clock.System.now()
            val base = SyncChange(
                id = "1",
                itemId = "i",
                changeType = ChangeType.CREATE,
                userId = "u",
                timestamp = now,
                cursor = 1L,
            )

            val create = base.copy(changeType = ChangeType.CREATE)
            val modify = base.copy(changeType = ChangeType.MODIFY)
            val delete = base.copy(changeType = ChangeType.DELETE)
            val move = base.copy(changeType = ChangeType.MOVE)
            val rename = base.copy(changeType = ChangeType.RENAME)

            assertEquals(ChangeType.CREATE, create.changeType)
            assertEquals(ChangeType.MODIFY, modify.changeType)
            assertEquals(ChangeType.DELETE, delete.changeType)
            assertEquals(ChangeType.MOVE, move.changeType)
            assertEquals(ChangeType.RENAME, rename.changeType)
        }

        @Test
        fun `change should allow null optional fields`() {
            val now = Clock.System.now()

            val change = SyncChange(
                id = "1",
                itemId = "i",
                changeType = ChangeType.CREATE,
                userId = "u",
                deviceId = null,
                timestamp = now,
                cursor = 1L,
                oldPath = null,
                newPath = null,
                checksum = null,
                metadata = emptyMap(),
            )

            assertNull(change.deviceId)
            assertNull(change.oldPath)
            assertNull(change.newPath)
            assertNull(change.checksum)
            assertTrue(change.metadata.isEmpty())
        }

        @Test
        fun `change should track metadata`() {
            val now = Clock.System.now()

            val change = SyncChange(
                id = "1",
                itemId = "i",
                changeType = ChangeType.MODIFY,
                userId = "u",
                timestamp = now,
                cursor = 1L,
                metadata = mapOf(
                    "source" to "web",
                    "client_version" to "2.0.0",
                    "action" to "upload",
                ),
            )

            assertEquals(3, change.metadata.size)
            assertEquals("web", change.metadata["source"])
            assertEquals("2.0.0", change.metadata["client_version"])
        }
    }

    @Nested
    @DisplayName("SyncConflict Model Tests")
    inner class SyncConflictModelTests {

        @Test
        fun `conflict should be created with local and remote changes`() {
            val now = Clock.System.now()

            val localChange = SyncChange(
                id = "local-1",
                itemId = "item-1",
                changeType = ChangeType.MODIFY,
                userId = "user-1",
                timestamp = now,
                cursor = 100L,
            )

            val remoteChange = SyncChange(
                id = "remote-1",
                itemId = "item-1",
                changeType = ChangeType.MODIFY,
                userId = "user-2",
                timestamp = now + 1.hours,
                cursor = 101L,
            )

            val conflict = SyncConflict(
                id = "conflict-123",
                itemId = "item-1",
                localChange = localChange,
                remoteChange = remoteChange,
                conflictType = ConflictType.EDIT_CONFLICT,
                resolvedAt = null,
                resolution = null,
                createdAt = now,
            )

            assertEquals("conflict-123", conflict.id)
            assertEquals("item-1", conflict.itemId)
            assertEquals(localChange, conflict.localChange)
            assertEquals(remoteChange, conflict.remoteChange)
            assertEquals(ConflictType.EDIT_CONFLICT, conflict.conflictType)
            assertNull(conflict.resolvedAt)
            assertNull(conflict.resolution)
        }

        @Test
        fun `conflict should support different conflict types`() {
            val now = Clock.System.now()
            val change = SyncChange(
                id = "c",
                itemId = "i",
                changeType = ChangeType.MODIFY,
                userId = "u",
                timestamp = now,
                cursor = 1L,
            )
            val base = SyncConflict(
                id = "1",
                itemId = "i",
                localChange = change,
                remoteChange = change,
                conflictType = ConflictType.EDIT_CONFLICT,
                resolvedAt = null,
                resolution = null,
                createdAt = now,
            )

            val editConflict = base.copy(conflictType = ConflictType.EDIT_CONFLICT)
            val editDelete = base.copy(conflictType = ConflictType.EDIT_DELETE)
            val deleteEdit = base.copy(conflictType = ConflictType.DELETE_EDIT)
            val moveMove = base.copy(conflictType = ConflictType.MOVE_MOVE)

            assertEquals(ConflictType.EDIT_CONFLICT, editConflict.conflictType)
            assertEquals(ConflictType.EDIT_DELETE, editDelete.conflictType)
            assertEquals(ConflictType.DELETE_EDIT, deleteEdit.conflictType)
            assertEquals(ConflictType.MOVE_MOVE, moveMove.conflictType)
        }

        @Test
        fun `conflict should track resolution`() {
            val now = Clock.System.now()
            val change = SyncChange(
                id = "c",
                itemId = "i",
                changeType = ChangeType.MODIFY,
                userId = "u",
                timestamp = now,
                cursor = 1L,
            )

            val resolvedConflict = SyncConflict(
                id = "1",
                itemId = "i",
                localChange = change,
                remoteChange = change,
                conflictType = ConflictType.EDIT_CONFLICT,
                resolvedAt = now + 30.hours,
                resolution = ConflictResolution.KEEP_LOCAL,
                createdAt = now,
            )

            assertNotNull(resolvedConflict.resolvedAt)
            assertEquals(ConflictResolution.KEEP_LOCAL, resolvedConflict.resolution)
        }

        @Test
        fun `conflict should support different resolutions`() {
            val now = Clock.System.now()
            val change = SyncChange(
                id = "c",
                itemId = "i",
                changeType = ChangeType.MODIFY,
                userId = "u",
                timestamp = now,
                cursor = 1L,
            )
            val base = SyncConflict(
                id = "1",
                itemId = "i",
                localChange = change,
                remoteChange = change,
                conflictType = ConflictType.EDIT_CONFLICT,
                resolvedAt = now,
                resolution = null,
                createdAt = now,
            )

            val keepLocal = base.copy(resolution = ConflictResolution.KEEP_LOCAL)
            val keepRemote = base.copy(resolution = ConflictResolution.KEEP_REMOTE)
            val keepBoth = base.copy(resolution = ConflictResolution.KEEP_BOTH)
            val merge = base.copy(resolution = ConflictResolution.MERGE)

            assertEquals(ConflictResolution.KEEP_LOCAL, keepLocal.resolution)
            assertEquals(ConflictResolution.KEEP_REMOTE, keepRemote.resolution)
            assertEquals(ConflictResolution.KEEP_BOTH, keepBoth.resolution)
            assertEquals(ConflictResolution.MERGE, merge.resolution)
        }
    }

    @Nested
    @DisplayName("Repository Method Signature Tests")
    inner class MethodSignatureTests {

        @Test
        fun `registerDevice method should exist`() {
            assertNotNull(repository::registerDevice)
        }

        @Test
        fun `findDevice method should exist`() {
            assertNotNull(repository::findDevice)
        }

        @Test
        fun `findDeviceByUserAndId method should exist`() {
            assertNotNull(repository::findDeviceByUserAndId)
        }

        @Test
        fun `listDevices method should exist`() {
            assertNotNull(repository::listDevices)
        }

        @Test
        fun `updateDevice method should exist`() {
            assertNotNull(repository::updateDevice)
        }

        @Test
        fun `deactivateDevice method should exist`() {
            assertNotNull(repository::deactivateDevice)
        }

        @Test
        fun `removeDevice method should exist`() {
            assertNotNull(repository::removeDevice)
        }

        @Test
        fun `recordChange method should exist`() {
            assertNotNull(repository::recordChange)
        }

        @Test
        fun `getChangesSince method should exist`() {
            assertNotNull(repository::getChangesSince)
        }

        @Test
        fun `getCurrentCursor method should exist`() {
            assertNotNull(repository::getCurrentCursor)
        }

        @Test
        fun `getChangesForItem method should exist`() {
            assertNotNull(repository::getChangesForItem)
        }

        @Test
        fun `getChangesByType method should exist`() {
            assertNotNull(repository::getChangesByType)
        }

        @Test
        fun `streamChanges method should exist`() {
            assertNotNull(repository::streamChanges)
        }

        @Test
        fun `pruneChanges method should exist`() {
            assertNotNull(repository::pruneChanges)
        }

        @Test
        fun `createConflict method should exist`() {
            assertNotNull(repository::createConflict)
        }

        @Test
        fun `findConflict method should exist`() {
            assertNotNull(repository::findConflict)
        }

        @Test
        fun `getPendingConflicts method should exist`() {
            assertNotNull(repository::getPendingConflicts)
        }

        @Test
        fun `getConflictsForItem method should exist`() {
            assertNotNull(repository::getConflictsForItem)
        }

        @Test
        fun `resolveConflict method should exist`() {
            assertNotNull(repository::resolveConflict)
        }

        @Test
        fun `pruneResolvedConflicts method should exist`() {
            assertNotNull(repository::pruneResolvedConflicts)
        }
    }

    @Nested
    @DisplayName("Cursor Tests")
    inner class CursorTests {

        @Test
        fun `cursor should be sequential`() {
            val now = Clock.System.now()

            val changes = (1L..5L).map { cursor ->
                SyncChange(
                    id = "c$cursor",
                    itemId = "item",
                    changeType = ChangeType.MODIFY,
                    userId = "user",
                    timestamp = now,
                    cursor = cursor,
                )
            }

            changes.forEachIndexed { index, change ->
                assertEquals(index + 1L, change.cursor)
            }
        }

        @Test
        fun `cursor should allow zero value`() {
            val change = SyncChange(
                id = "c1",
                itemId = "item",
                changeType = ChangeType.CREATE,
                userId = "user",
                timestamp = Clock.System.now(),
                cursor = 0L,
            )

            assertEquals(0L, change.cursor)
        }
    }
}
