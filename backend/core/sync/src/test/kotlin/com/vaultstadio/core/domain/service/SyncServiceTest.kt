/**
 * VaultStadio Sync Service Tests
 */

package com.vaultstadio.core.domain.service

import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.ChangeType
import com.vaultstadio.core.domain.model.ConflictResolution
import com.vaultstadio.core.domain.model.ConflictType
import com.vaultstadio.core.domain.model.DeviceType
import com.vaultstadio.core.domain.model.SyncChange
import com.vaultstadio.core.domain.model.SyncConflict
import com.vaultstadio.core.domain.model.SyncDevice
import com.vaultstadio.core.domain.model.SyncRequest
import com.vaultstadio.core.domain.repository.SyncRepository
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.InvalidOperationException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
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

    @Test
    fun `registerDevice should propagate error when findDeviceByUserAndId returns Left`() = runTest {
        val userId = "user-1"
        val input = RegisterDeviceInput(
            deviceId = "device-123",
            deviceName = "My Laptop",
            deviceType = DeviceType.DESKTOP_MAC,
        )
        val repoError = DatabaseException("DB error")
        coEvery { syncRepository.findDeviceByUserAndId(userId, input.deviceId) } returns repoError.left()

        val result = service.registerDevice(input, userId)

        assertTrue(result.isLeft())
        result.onLeft { err -> assertTrue(err is DatabaseException) }
    }

    @Test
    fun `listDevices should propagate error when repository returns Left`() = runTest {
        val userId = "user-1"
        val repoError = DatabaseException("DB error")
        coEvery { syncRepository.listDevices(userId, true) } returns repoError.left()

        val result = service.listDevices(userId, true)

        assertTrue(result.isLeft())
        result.onLeft { err -> assertTrue(err is DatabaseException) }
    }

    @Test
    fun `recordChange should propagate error when getCurrentCursor returns Left`() = runTest {
        val userId = "user-1"
        val input = RecordChangeInput(
            itemId = "item-1",
            changeType = ChangeType.MODIFY,
            deviceId = "device-1",
            newPath = "/new/path.txt",
            checksum = "abc123",
        )
        val repoError = DatabaseException("DB error")
        coEvery { syncRepository.getCurrentCursor(userId) } returns repoError.left()

        val result = service.recordChange(input, userId)

        assertTrue(result.isLeft())
    }

    @Test
    fun `deactivateDevice should return ItemNotFoundException when device not found`() = runTest {
        val userId = "user-1"
        val deviceId = "unknown-device"
        coEvery { syncRepository.findDeviceByUserAndId(userId, deviceId) } returns null.right()

        val result = service.deactivateDevice(deviceId, userId)

        assertTrue(result.isLeft())
        result.onLeft { err ->
            assertTrue(err is ItemNotFoundException)
            assertTrue(err.message.contains("Device not found"))
        }
    }

    @Test
    fun `removeDevice should return ItemNotFoundException when device not found`() = runTest {
        val userId = "user-1"
        val deviceId = "unknown-device"
        coEvery { syncRepository.findDeviceByUserAndId(userId, deviceId) } returns null.right()

        val result = service.removeDevice(deviceId, userId)

        assertTrue(result.isLeft())
        result.onLeft { err ->
            assertTrue(err is ItemNotFoundException)
            assertTrue(err.message.contains("Device not found"))
        }
    }

    @Test
    fun `sync should return ItemNotFoundException when device not found`() = runTest {
        val userId = "user-1"
        val request = SyncRequest(deviceId = "unknown-device")
        coEvery { syncRepository.findDeviceByUserAndId(userId, request.deviceId) } returns null.right()

        val result = service.sync(request, userId)

        assertTrue(result.isLeft())
        result.onLeft { err ->
            assertTrue(err is ItemNotFoundException)
            assertTrue(err.message.contains("Device not found"))
        }
    }

    @Test
    fun `sync should return InvalidOperationException when device is not active`() = runTest {
        val userId = "user-1"
        val now = Clock.System.now()
        val inactiveDevice = SyncDevice(
            id = "dev-1",
            userId = userId,
            deviceId = "device-1",
            deviceName = "Laptop",
            deviceType = DeviceType.DESKTOP_MAC,
            isActive = false,
            createdAt = now,
            updatedAt = now,
        )
        val request = SyncRequest(deviceId = "device-1")
        coEvery { syncRepository.findDeviceByUserAndId(userId, request.deviceId) } returns inactiveDevice.right()

        val result = service.sync(request, userId)

        assertTrue(result.isLeft())
        result.onLeft { err ->
            assertTrue(err is InvalidOperationException)
            assertTrue(err.message.contains("not active"))
        }
    }

    @Test
    fun `sync should return changes and cursor when device is active`() = runTest {
        val userId = "user-1"
        val now = Clock.System.now()
        val device = SyncDevice(
            id = "dev-1",
            userId = userId,
            deviceId = "device-1",
            deviceName = "Laptop",
            deviceType = DeviceType.DESKTOP_MAC,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        val request = SyncRequest(deviceId = "device-1", cursor = null, limit = 10)
        coEvery { syncRepository.findDeviceByUserAndId(userId, request.deviceId) } returns device.right()
        coEvery { syncRepository.getChangesSince(userId, 0L, request.limit) } returns emptyList<SyncChange>().right()
        coEvery { syncRepository.getPendingConflicts(userId) } returns emptyList<SyncConflict>().right()
        coEvery { syncRepository.getCurrentCursor(userId) } returns 5L.right()
        coEvery { syncRepository.updateDevice(any()) } answers { firstArg<SyncDevice>().right() }

        val result = service.sync(request, userId)

        assertTrue(result.isRight())
        result.onRight { response ->
            assertEquals(emptyList(), response.changes)
            assertEquals("5", response.cursor)
            assertEquals(emptyList(), response.conflicts)
        }
    }

    @Test
    fun `getPendingConflicts should propagate error when repository returns Left`() = runTest {
        val userId = "user-1"
        val repoError = DatabaseException("DB error")
        coEvery { syncRepository.getPendingConflicts(userId) } returns repoError.left()

        val result = service.getPendingConflicts(userId)

        assertTrue(result.isLeft())
    }

    @Test
    fun `resolveConflict should return ItemNotFoundException when conflict not found`() = runTest {
        val conflictId = "missing-conflict"
        val userId = "user-1"
        coEvery { syncRepository.findConflict(conflictId) } returns null.right()

        val result = service.resolveConflict(conflictId, ConflictResolution.KEEP_LOCAL, userId)

        assertTrue(result.isLeft())
        result.onLeft { err ->
            assertTrue(err is ItemNotFoundException)
            assertTrue(err.message.contains("Conflict not found"))
        }
    }

    @Test
    fun `resolveConflict should return InvalidOperationException when conflict already resolved`() = runTest {
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
        val resolvedConflict = SyncConflict(
            id = conflictId,
            itemId = "item-1",
            localChange = localChange,
            remoteChange = remoteChange,
            conflictType = ConflictType.EDIT_CONFLICT,
            resolvedAt = now,
            resolution = ConflictResolution.KEEP_REMOTE,
            createdAt = now,
        )
        coEvery { syncRepository.findConflict(conflictId) } returns resolvedConflict.right()

        val result = service.resolveConflict(conflictId, ConflictResolution.KEEP_LOCAL, userId)

        assertTrue(result.isLeft())
        result.onLeft { err ->
            assertTrue(err is InvalidOperationException)
            assertTrue(err.message.contains("already resolved"))
        }
    }

    @Test
    fun `pruneOldData should return sum of pruned changes and conflicts`() = runTest {
        coEvery { syncRepository.pruneChanges(any()) } returns 10.right()
        coEvery { syncRepository.pruneResolvedConflicts(any()) } returns 3.right()

        val result = service.pruneOldData(olderThanDays = 30)

        assertTrue(result.isRight())
        result.onRight { total -> assertEquals(13, total) }
    }

    @Test
    fun `generateFileSignature should return signature with empty blocks`() = runTest {
        val result = service.generateFileSignature("item-1", versionNumber = 1, blockSize = 4096)

        assertTrue(result.isRight())
        result.onRight { sig ->
            assertEquals("item-1", sig.itemId)
            assertEquals(1, sig.versionNumber)
            assertEquals(4096, sig.blockSize)
            assertTrue(sig.blocks.isEmpty())
        }
    }

    @Test
    fun `pushChanges should propagate error when getChangesForItem returns Left`() = runTest {
        val userId = "user-1"
        val changes = listOf(
            RecordChangeInput(
                itemId = "item-1",
                changeType = ChangeType.MODIFY,
                deviceId = "device-1",
                newPath = "/path.txt",
                checksum = "c1",
            ),
        )
        val repoError = DatabaseException("DB error")
        coEvery { syncRepository.getChangesForItem("item-1", 1) } returns repoError.left()

        val result = service.pushChanges(changes, "device-1", userId)

        assertTrue(result.isLeft())
        result.onLeft { err -> assertTrue(err is DatabaseException) }
    }

    @Test
    fun `pushChanges should return empty conflicts when no conflicts and recordChange succeeds`() = runTest {
        val userId = "user-1"
        val changes = listOf(
            RecordChangeInput(
                itemId = "item-1",
                changeType = ChangeType.MODIFY,
                deviceId = "device-1",
                newPath = "/path.txt",
                checksum = "c1",
            ),
        )
        coEvery { syncRepository.getChangesForItem("item-1", 1) } returns emptyList<SyncChange>().right()
        coEvery { syncRepository.getCurrentCursor(userId) } returns 0L.right()
        coEvery { syncRepository.recordChange(any()) } answers { firstArg<SyncChange>().right() }

        val result = service.pushChanges(changes, "device-1", userId)

        assertTrue(result.isRight())
        result.onRight { list -> assertTrue(list.isEmpty()) }
    }

    @Test
    fun `pushChanges should return empty list when changes list is empty`() = runTest {
        val result = service.pushChanges(emptyList(), "device-1", "user-1")

        assertTrue(result.isRight())
        result.onRight { list -> assertTrue(list.isEmpty()) }
    }
}
