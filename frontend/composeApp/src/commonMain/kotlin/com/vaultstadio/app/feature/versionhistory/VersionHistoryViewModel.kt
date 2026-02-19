package com.vaultstadio.app.feature.versionhistory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.model.FileVersion
import com.vaultstadio.app.domain.model.FileVersionHistory
import com.vaultstadio.app.domain.model.VersionDiff
import com.vaultstadio.app.domain.usecase.config.GetVersionUrlsUseCase
import com.vaultstadio.app.domain.usecase.version.CleanupVersionsUseCase
import com.vaultstadio.app.domain.usecase.version.CompareVersionsUseCase
import com.vaultstadio.app.domain.usecase.version.DeleteVersionUseCase
import com.vaultstadio.app.domain.usecase.version.GetVersionHistoryUseCase
import com.vaultstadio.app.domain.usecase.version.GetVersionUseCase
import com.vaultstadio.app.domain.usecase.version.RestoreVersionUseCase
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

/**
 * ViewModel for version history management.
 */
@KoinViewModel
class VersionHistoryViewModel(
    private val getVersionHistoryUseCase: GetVersionHistoryUseCase,
    private val getVersionUseCase: GetVersionUseCase,
    private val restoreVersionUseCase: RestoreVersionUseCase,
    private val compareVersionsUseCase: CompareVersionsUseCase,
    private val deleteVersionUseCase: DeleteVersionUseCase,
    private val cleanupVersionsUseCase: CleanupVersionsUseCase,
    private val getVersionUrlsUseCase: GetVersionUrlsUseCase,
    @InjectedParam private val initialItemId: String,
) : ViewModel() {

    var versionHistory by mutableStateOf<FileVersionHistory?>(null)
        private set
    var selectedVersion by mutableStateOf<FileVersion?>(null)
        private set
    var versionDiff by mutableStateOf<VersionDiff?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var downloadUrl by mutableStateOf<String?>(null)
        private set
    private var currentItemId: String = initialItemId

    init {
        loadHistory(initialItemId)
    }

    fun loadHistory(itemId: String) {
        currentItemId = itemId
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = getVersionHistoryUseCase(itemId)) {
                is ApiResult.Success -> versionHistory = result.data
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
            isLoading = false
        }
    }

    fun getVersion(itemId: String, versionNumber: Int) {
        viewModelScope.launch {
            error = null
            when (val result = getVersionUseCase(itemId, versionNumber)) {
                is ApiResult.Success -> selectedVersion = result.data
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun deleteVersion(versionId: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = deleteVersionUseCase(versionId)) {
                is ApiResult.Success -> loadHistory(currentItemId)
                is ApiResult.Error -> {
                    error = result.message
                    isLoading = false
                }
                is ApiResult.NetworkError -> {
                    error = result.message
                    isLoading = false
                }
            }
        }
    }

    fun clearSelectedVersion() {
        selectedVersion = null
    }

    fun restoreVersion(itemId: String, versionNumber: Int, comment: String?) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = restoreVersionUseCase(itemId, versionNumber, comment)) {
                is ApiResult.Success -> loadHistory(itemId)
                is ApiResult.Error -> {
                    error = result.message
                    isLoading = false
                }
                is ApiResult.NetworkError -> {
                    error = result.message
                    isLoading = false
                }
            }
        }
    }

    fun compareVersions(itemId: String, fromVersion: Int, toVersion: Int) {
        viewModelScope.launch {
            error = null
            when (val result = compareVersionsUseCase(itemId, fromVersion, toVersion)) {
                is ApiResult.Success -> versionDiff = result.data
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun downloadVersion(itemId: String, versionNumber: Int) {
        downloadUrl = getVersionUrlsUseCase.downloadUrl(itemId, versionNumber)
    }

    fun clearDownloadUrl() {
        downloadUrl = null
    }

    fun cleanupVersions(itemId: String, maxVersions: Int?, maxAgeDays: Int?, minKeep: Int) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = cleanupVersionsUseCase(itemId, maxVersions, maxAgeDays, minKeep)) {
                is ApiResult.Success -> loadHistory(itemId)
                is ApiResult.Error -> {
                    error = result.message
                    isLoading = false
                }
                is ApiResult.NetworkError -> {
                    error = result.message
                    isLoading = false
                }
            }
        }
    }

    fun clearDiff() {
        versionDiff = null
    }

    fun clearError() {
        error = null
    }
}
