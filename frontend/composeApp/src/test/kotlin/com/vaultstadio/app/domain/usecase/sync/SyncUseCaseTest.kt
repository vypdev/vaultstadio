/**
 * Unit tests for sync use cases (GetDevices, RegisterDevice, DeactivateDevice, RemoveDevice, etc.).
 * Uses a fake SyncRepository to avoid platform/DI.
 */

package com.vaultstadio.app.domain.usecase.sync

import com.vaultstadio.app.data.sync.usecase.DeactivateDeviceUseCaseImpl
import com.vaultstadio.app.data.sync.usecase.GetConflictsUseCaseImpl
import com.vaultstadio.app.data.sync.usecase.GetDevicesUseCaseImpl
import com.vaultstadio.app.data.sync.usecase.PullChangesUseCaseImpl
import com.vaultstadio.app.data.sync.usecase.RegisterDeviceUseCaseImpl
import com.vaultstadio.app.data.sync.usecase.RemoveDeviceUseCaseImpl
import com.vaultstadio.app.data.sync.usecase.ResolveConflictUseCaseImpl
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.sync.SyncRepository
import com.vaultstadio.app.domain.sync.model.ConflictResolution
import com.vaultstadio.app.domain.sync.model.DeviceType
import com.vaultstadio.app.domain.sync.model.SyncConflict
import com.vaultstadio.app.domain.sync.model.SyncDevice
import com.vaultstadio.app.domain.sync.model.SyncResponse
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testSyncDevice(
    id: String = "dev-1",
    deviceId: String = "device-id",
    deviceName: String = "My Device",
    deviceType: DeviceType = DeviceType.DESKTOP_MAC,
) = SyncDevice(
    id = id,
    deviceId = deviceId,
    deviceName = deviceName,
    deviceType = deviceType,
    lastSyncAt = null,
    isActive = true,
    createdAt = testInstant,
)

private class FakeSyncRepository(
    var getDevicesResult: Result<List<SyncDevice>> = Result.success(emptyList()),
    var registerDeviceResult: Result<SyncDevice> = Result.success(testSyncDevice()),
    var deactivateDeviceResult: Result<Unit> = Result.success(Unit),
    var removeDeviceResult: Result<Unit> = Result.success(Unit),
    var pullChangesResult: Result<SyncResponse> = Result.success(
        SyncResponse(emptyList(), "", false, emptyList(), testInstant),
    ),
    var getConflictsResult: Result<List<SyncConflict>> = Result.success(emptyList()),
    var resolveConflictResult: Result<Unit> = Result.success(Unit),
) : SyncRepository {

    override suspend fun registerDevice(
        deviceId: String,
        deviceName: String,
        deviceType: DeviceType,
    ): Result<SyncDevice> = registerDeviceResult

    override suspend fun getDevices(activeOnly: Boolean): Result<List<SyncDevice>> = getDevicesResult

    override suspend fun deactivateDevice(deviceId: String): Result<Unit> = deactivateDeviceResult

    override suspend fun removeDevice(deviceId: String): Result<Unit> = removeDeviceResult

    override suspend fun pullChanges(
        deviceId: String,
        cursor: String?,
        limit: Int,
        includeDeleted: Boolean,
    ): Result<SyncResponse> = pullChangesResult

    override suspend fun getConflicts(): Result<List<SyncConflict>> = getConflictsResult

    override suspend fun resolveConflict(conflictId: String, resolution: ConflictResolution): Result<Unit> =
        resolveConflictResult
}

class GetDevicesUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetDevicesResult() = runTest {
        val devices = listOf(testSyncDevice("d1"), testSyncDevice("d2"))
        val repo = FakeSyncRepository(getDevicesResult = Result.success(devices))
        val useCase = GetDevicesUseCaseImpl(repo)
        val result = useCase(activeOnly = true)
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.size)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeSyncRepository(getDevicesResult = Result.error("UNAUTHORIZED", "Not logged in"))
        val useCase = GetDevicesUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class RegisterDeviceUseCaseTest {

    @Test
    fun invoke_returnsRepositoryRegisterDeviceResult() = runTest {
        val device = testSyncDevice(id = "new-dev", deviceName = "Laptop")
        val repo = FakeSyncRepository(registerDeviceResult = Result.success(device))
        val useCase = RegisterDeviceUseCaseImpl(repo)
        val result = useCase("dev-123", "Laptop", DeviceType.DESKTOP_MAC)
        assertTrue(result.isSuccess())
        assertEquals(device, result.getOrNull())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeSyncRepository(registerDeviceResult = Result.error("LIMIT", "Too many devices"))
        val useCase = RegisterDeviceUseCaseImpl(repo)
        val result = useCase("x", "y", DeviceType.MOBILE_ANDROID)
        assertTrue(result.isError())
    }
}

class DeactivateDeviceUseCaseTest {

    @Test
    fun invoke_returnsRepositoryDeactivateDeviceResult() = runTest {
        val repo = FakeSyncRepository(deactivateDeviceResult = Result.success(Unit))
        val useCase = DeactivateDeviceUseCaseImpl(repo)
        val result = useCase("device-1")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeSyncRepository(deactivateDeviceResult = Result.error("NOT_FOUND", "Device not found"))
        val useCase = DeactivateDeviceUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
    }
}

class RemoveDeviceUseCaseTest {

    @Test
    fun invoke_returnsRepositoryRemoveDeviceResult() = runTest {
        val repo = FakeSyncRepository(removeDeviceResult = Result.success(Unit))
        val useCase = RemoveDeviceUseCaseImpl(repo)
        val result = useCase("device-1")
        assertTrue(result.isSuccess())
    }
}

class GetConflictsUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetConflictsResult() = runTest {
        val repo = FakeSyncRepository(getConflictsResult = Result.success(emptyList()))
        val useCase = GetConflictsUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
        assertEquals(0, result.getOrNull()?.size)
    }
}

class ResolveConflictUseCaseTest {

    @Test
    fun invoke_returnsRepositoryResolveConflictResult() = runTest {
        val repo = FakeSyncRepository(resolveConflictResult = Result.success(Unit))
        val useCase = ResolveConflictUseCaseImpl(repo)
        val result = useCase("conflict-1", ConflictResolution.KEEP_LOCAL)
        assertTrue(result.isSuccess())
    }
}
