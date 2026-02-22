/**
 * Unit tests for SyncViewModel: loadDevices, loadConflicts, clearError, clearSyncResponse.
 */

package com.vaultstadio.app.feature.sync

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.sync.model.ConflictResolution
import com.vaultstadio.app.domain.sync.model.DeviceType
import com.vaultstadio.app.domain.sync.model.SyncConflict
import com.vaultstadio.app.domain.sync.model.SyncDevice
import com.vaultstadio.app.domain.sync.model.SyncResponse
import com.vaultstadio.app.domain.sync.usecase.DeactivateDeviceUseCase
import com.vaultstadio.app.domain.sync.usecase.GetConflictsUseCase
import com.vaultstadio.app.domain.sync.usecase.GetDevicesUseCase
import com.vaultstadio.app.domain.sync.usecase.PullChangesUseCase
import com.vaultstadio.app.domain.sync.usecase.RegisterDeviceUseCase
import com.vaultstadio.app.domain.sync.usecase.RemoveDeviceUseCase
import com.vaultstadio.app.domain.sync.usecase.ResolveConflictUseCase
import com.vaultstadio.app.feature.ViewModelTestBase
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testSyncDevice(id: String = "dev-1") = SyncDevice(
    id = id,
    deviceId = "device-$id",
    deviceName = "My Device",
    deviceType = DeviceType.DESKTOP_MAC,
    lastSyncAt = testInstant,
    isActive = true,
    createdAt = testInstant,
)

private class FakeGetDevicesUseCase(
    var result: Result<List<SyncDevice>> = Result.success(emptyList()),
) : GetDevicesUseCase {
    override suspend fun invoke(activeOnly: Boolean): Result<List<SyncDevice>> = result
}

private class FakeGetConflictsUseCase(
    var result: Result<List<SyncConflict>> = Result.success(emptyList()),
) : GetConflictsUseCase {
    override suspend fun invoke(): Result<List<SyncConflict>> = result
}

private class FakeRegisterDeviceUseCase(
    var result: Result<SyncDevice> = Result.success(testSyncDevice()),
) : RegisterDeviceUseCase {
    override suspend fun invoke(
        deviceId: String,
        deviceName: String,
        deviceType: DeviceType,
    ): Result<SyncDevice> = result
}

private class FakeDeactivateDeviceUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : DeactivateDeviceUseCase {
    override suspend fun invoke(deviceId: String): Result<Unit> = result
}

private class FakeRemoveDeviceUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : RemoveDeviceUseCase {
    override suspend fun invoke(deviceId: String): Result<Unit> = result
}

private class FakeResolveConflictUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : ResolveConflictUseCase {
    override suspend fun invoke(conflictId: String, resolution: ConflictResolution): Result<Unit> = result
}

private class FakePullChangesUseCase(
    var result: Result<SyncResponse> = Result.success(
        SyncResponse(
            changes = emptyList(),
            cursor = "cursor1",
            hasMore = false,
            conflicts = emptyList(),
            serverTime = testInstant,
        ),
    ),
) : PullChangesUseCase {
    override suspend fun invoke(
        deviceId: String,
        cursor: String?,
        limit: Int,
        includeDeleted: Boolean,
    ): Result<SyncResponse> = result
}

class SyncViewModelTest {

    private fun createViewModel(
        getDevicesResult: Result<List<SyncDevice>> = Result.success(emptyList()),
        getConflictsResult: Result<List<SyncConflict>> = Result.success(emptyList()),
    ): SyncViewModel = SyncViewModel(
        getDevicesUseCase = FakeGetDevicesUseCase(getDevicesResult),
        getConflictsUseCase = FakeGetConflictsUseCase(getConflictsResult),
        registerDeviceUseCase = FakeRegisterDeviceUseCase(),
        deactivateDeviceUseCase = FakeDeactivateDeviceUseCase(),
        removeDeviceUseCase = FakeRemoveDeviceUseCase(),
        resolveConflictUseCase = FakeResolveConflictUseCase(),
        pullChangesUseCase = FakePullChangesUseCase(),
    )

    @Test
    fun loadDevices_success_populatesDevices() = ViewModelTestBase.runTestWithMain {
        val devices = listOf(testSyncDevice("d1"), testSyncDevice("d2"))
        val vm = createViewModel(getDevicesResult = Result.success(devices))
        vm.loadDevices()
        assertEquals(devices, vm.devices)
        assertNull(vm.error)
    }

    @Test
    fun loadDevices_error_setsError() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getDevicesResult = Result.error("ERR", "Load devices failed"))
        vm.loadDevices()
        assertTrue(vm.devices.isEmpty())
        assertEquals("Load devices failed", vm.error)
    }

    @Test
    fun loadConflicts_success_populatesConflicts() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getConflictsResult = Result.success(emptyList()))
        vm.loadConflicts()
        assertTrue(vm.conflicts.isEmpty())
        assertNull(vm.error)
    }

    @Test
    fun loadConflicts_error_setsError() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getConflictsResult = Result.error("ERR", "Load conflicts failed"))
        vm.loadConflicts()
        assertEquals("Load conflicts failed", vm.error)
    }

    @Test
    fun clearError_clearsErrorMessage() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getDevicesResult = Result.error("ERR", "Oops"))
        vm.loadDevices()
        assertEquals("Oops", vm.error)
        vm.clearError()
        assertNull(vm.error)
    }

    @Test
    fun clearSyncResponse_clearsSyncResponse() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel()
        vm.pullChanges("dev-1", null)
        assertTrue(vm.syncResponse != null)
        vm.clearSyncResponse()
        assertNull(vm.syncResponse)
    }
}
