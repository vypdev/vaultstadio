/**
 * VaultStadio Sync Service Tests
 */

package com.vaultstadio.core.domain.service

import arrow.core.right
import com.vaultstadio.core.domain.model.ChangeType
import com.vaultstadio.core.domain.model.ConflictResolution
import com.vaultstadio.core.domain.model.ConflictType
import com.vaultstadio.core.domain.model.DeviceType
import com.vaultstadio.core.domain.model.SyncChange
import com.vaultstadio.core.domain.model.SyncConflict
import com.vaultstadio.core.domain.model.SyncDevice
import com.vaultstadio.core.domain.repository.SyncRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncServiceTest {

    private lateinit var syncRepository: SyncRepository
    private lateinit var service: SyncService

    @BeforeEach
    fun setup() {
        syncRepository = mockk()
        service = SyncService(syncRepository)
    }

    @Test
    fun `registerDevice should create new device`() = runTest {
        val userId = "user-1"
        val input = RegisterDeviceInput(
            deviceId = "device-123",
            deviceName = "My Laptop",
            deviceType = DeviceType.DESKTOP_MAC,
        )

        coEvery { syncRepository.findDeviceByUserAndId(userId, input.deviceId) } returns null.right()
        coEvery { syncRepository.registerDevice(any()) } answers {
            firstArg<SyncDevice>().right()
        }

        val result = service.registerDevice(input, userId)

        assertTrue(result.isRight())
        result.onRight { device ->
            assertEquals(input.deviceId, device.deviceId)
            assertEquals(input.deviceName, device.deviceName)
            assertEquals(input.deviceType, device.deviceType)
            assertTrue(device.isActive)
        }
    }

    @Test
    fun `registerDevice should reactivate existing device`() = runTest {
        val userId = "user-1"
        val now = Clock.System.now()

        val existingDevice = SyncDevice(
            id = "existing-id",
            userId = userId,
            deviceId = "device-123",
            deviceName = "Old Name",
            deviceType = DeviceType.DESKTOP_MAC,
            isActive = false,
            createdAt = now,
            updatedAt = now,
        )

        val input = RegisterDeviceInput(
            deviceId = "device-123",
            deviceName = "New Name",
            deviceType = DeviceType.DESKTOP_MAC,
        )

        coEvery { syncRepository.findDeviceByUserAndId(userId, input.deviceId) } returns existingDevice.right()
        coEvery { syncRepository.updateDevice(any()) } answers {
            firstArg<SyncDevice>().right()
        }

        val result = service.registerDevice(input, userId)

        assertTrue(result.isRight())
        result.onRight { device ->
            assertEquals("New Name", device.deviceName)
            assertTrue(device.isActive)
        }
    }

    @Test
    fun `listDevices should return user devices`() = runTest {
        val userId = "user-1"
        val now = Clock.System.now()

        val devices = listOf(
            SyncDevice(
                id = "d1",
                userId = userId,
                deviceId = "device-1",
                deviceName = "Laptop",
                deviceType = DeviceType.DESKTOP_MAC,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            ),
            SyncDevice(
                id = "d2",
                userId = userId,
                deviceId = "device-2",
                deviceName = "Phone",
                deviceType = DeviceType.MOBILE_IOS,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            ),
        )

        coEvery { syncRepository.listDevices(userId, true) } returns devices.right()

        val result = service.listDevices(userId, true)

        assertTrue(result.isRight())
        result.onRight { list ->
            assertEquals(2, list.size)
        }
    }

    @Test
    fun `recordChange should create change with cursor`() = runTest {
        val userId = "user-1"
        val input = RecordChangeInput(
            itemId = "item-1",
            changeType = ChangeType.MODIFY,
            deviceId = "device-1",
            newPath = "/new/path.txt",
            checksum = "abc123",
        )

        coEvery { syncRepository.getCurrentCursor(userId) } returns 100L.right()
        coEvery { syncRepository.recordChange(any()) } answers {
            firstArg<SyncChange>().right()
        }

        val result = service.recordChange(input, userId)

        assertTrue(result.isRight())
        result.onRight { change ->
            assertEquals("item-1", change.itemId)
            assertEquals(ChangeType.MODIFY, change.changeType)
            assertEquals(101L, change.cursor)
        }
    }

    @Test
    fun `getPendingConflicts should return unresolved conflicts`() = runTest {
        val userId = "user-1"
        val now = Clock.System.now()

        val localChange = SyncChange(
            id = "local-1",
            itemId = "item-1",
            changeType = ChangeType.MODIFY,
            userId = userId,
            timestamp = now,
            cursor = 100,
        )

        val remoteChange = SyncChange(
            id = "remote-1",
            itemId = "item-1",
            changeType = ChangeType.MODIFY,
            userId = userId,
            timestamp = now,
            cursor = 101,
        )

        val conflicts = listOf(
            SyncConflict(
                id = "conflict-1",
                itemId = "item-1",
                localChange = localChange,
                remoteChange = remoteChange,
                conflictType = ConflictType.EDIT_CONFLICT,
                createdAt = now,
            ),
        )

        coEvery { syncRepository.getPendingConflicts(userId) } returns conflicts.right()

        val result = service.getPendingConflicts(userId)

        assertTrue(result.isRight())
        result.onRight { list ->
            assertEquals(1, list.size)
            assertTrue(list.first().isPending)
        }
    }

    @Test
    fun `resolveConflict should resolve pending conflict`() = runTest {
        val conflictId = "conflict-1"
        val userId = "user-1"
        val now = Clock.System.now()

        val localChange = SyncChange(
            id = "local-1",
            itemId = "item-1",
            changeType = ChangeType.MODIFY,
            userId = userId,
            timestamp = now,
            cursor = 100,
        )

        val remoteChange = SyncChange(
            id = "remote-1",
            itemId = "item-1",
            changeType = ChangeType.MODIFY,
            userId = userId,
            timestamp = now,
            cursor = 101,
        )

        val pendingConflict = SyncConflict(
            id = conflictId,
            itemId = "item-1",
            localChange = localChange,
            remoteChange = remoteChange,
            conflictType = ConflictType.EDIT_CONFLICT,
            createdAt = now,
        )

        val resolvedConflict = pendingConflict.copy(
            resolvedAt = now,
            resolution = ConflictResolution.KEEP_LOCAL,
        )

        coEvery { syncRepository.findConflict(conflictId) } returns pendingConflict.right()
        coEvery { syncRepository.resolveConflict(conflictId, ConflictResolution.KEEP_LOCAL, any()) } returns
            resolvedConflict.right()

        val result = service.resolveConflict(conflictId, ConflictResolution.KEEP_LOCAL, userId)

        assertTrue(result.isRight())
        result.onRight { conflict ->
            assertEquals(ConflictResolution.KEEP_LOCAL, conflict.resolution)
        }
    }
}
