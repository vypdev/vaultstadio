package com.vaultstadio.app.feature.sync

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch

class SyncViewModel(
    private val getDevicesUseCase: GetDevicesUseCase,
    private val getConflictsUseCase: GetConflictsUseCase,
    private val registerDeviceUseCase: RegisterDeviceUseCase,
    private val deactivateDeviceUseCase: DeactivateDeviceUseCase,
    private val removeDeviceUseCase: RemoveDeviceUseCase,
    private val resolveConflictUseCase: ResolveConflictUseCase,
    private val pullChangesUseCase: PullChangesUseCase,
) : ViewModel() {

    var devices by mutableStateOf<List<SyncDevice>>(emptyList())
        private set
    var conflicts by mutableStateOf<List<SyncConflict>>(emptyList())
        private set
    var syncResponse by mutableStateOf<SyncResponse?>(null)
        private set
    var isSyncing by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadDevices()
        loadConflicts()
    }

    fun loadDevices() {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = getDevicesUseCase()) {
                is Result.Success -> devices = result.data
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
            isLoading = false
        }
    }

    fun loadConflicts() {
        viewModelScope.launch {
            error = null
            when (val result = getConflictsUseCase()) {
                is Result.Success -> conflicts = result.data
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun registerDevice(deviceId: String, deviceName: String, deviceType: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            val type = try {
                DeviceType.valueOf(deviceType.uppercase())
            } catch (e: IllegalArgumentException) {
                DeviceType.OTHER
            }
            when (val result = registerDeviceUseCase(deviceId, deviceName, type)) {
                is Result.Success -> loadDevices()
                is Result.Error -> {
                    error = result.message
                    isLoading = false
                }
                is Result.NetworkError -> {
                    error = result.message
                    isLoading = false
                }
            }
        }
    }

    fun deactivateDevice(deviceId: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = deactivateDeviceUseCase(deviceId)) {
                is Result.Success -> loadDevices()
                is Result.Error -> {
                    error = result.message
                    isLoading = false
                }
                is Result.NetworkError -> {
                    error = result.message
                    isLoading = false
                }
            }
        }
    }

    fun removeDevice(deviceId: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = removeDeviceUseCase(deviceId)) {
                is Result.Success -> loadDevices()
                is Result.Error -> {
                    error = result.message
                    isLoading = false
                }
                is Result.NetworkError -> {
                    error = result.message
                    isLoading = false
                }
            }
        }
    }

    fun resolveConflict(conflictId: String, resolution: ConflictResolution) {
        viewModelScope.launch {
            error = null
            when (val result = resolveConflictUseCase(conflictId, resolution)) {
                is Result.Success -> loadConflicts()
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun pullChanges(deviceId: String, cursor: String? = null) {
        viewModelScope.launch {
            isSyncing = true
            error = null
            when (val result = pullChangesUseCase(deviceId, cursor)) {
                is Result.Success -> {
                    syncResponse = result.data
                    loadConflicts()
                }
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
            isSyncing = false
        }
    }

    fun clearSyncResponse() {
        syncResponse = null
    }

    fun clearError() {
        error = null
    }
}
