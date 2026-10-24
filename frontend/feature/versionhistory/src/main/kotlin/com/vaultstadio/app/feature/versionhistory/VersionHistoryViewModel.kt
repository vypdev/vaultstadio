package com.vaultstadio.app.feature.versionhistory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.domain.config.usecase.GetVersionUrlsUseCase
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.version.model.FileVersion
import com.vaultstadio.app.domain.version.model.FileVersionHistory
import com.vaultstadio.app.domain.version.model.VersionDiff
import com.vaultstadio.app.domain.version.usecase.CleanupVersionsUseCase
import com.vaultstadio.app.domain.version.usecase.CompareVersionsUseCase
import com.vaultstadio.app.domain.version.usecase.DeleteVersionUseCase
import com.vaultstadio.app.domain.version.usecase.GetVersionHistoryUseCase
import com.vaultstadio.app.domain.version.usecase.GetVersionUseCase
import com.vaultstadio.app.domain.version.usecase.RestoreVersionUseCase
import kotlinx.coroutines.launch

class VersionHistoryViewModel(
    private val getVersionHistoryUseCase: GetVersionHistoryUseCase,
    private val getVersionUseCase: GetVersionUseCase,
    private val restoreVersionUseCase: RestoreVersionUseCase,
    private val compareVersionsUseCase: CompareVersionsUseCase,
    private val deleteVersionUseCase: DeleteVersionUseCase,
    private val cleanupVersionsUseCase: CleanupVersionsUseCase,
    private val getVersionUrlsUseCase: GetVersionUrlsUseCase,
    private val initialItemId: String,
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
                is Result.Success -> versionHistory = result.data
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
            isLoading = false
        }
    }

    fun getVersion(itemId: String, versionNumber: Int) {
        viewModelScope.launch {
            error = null
            when (val result = getVersionUseCase(itemId, versionNumber)) {
                is Result.Success -> selectedVersion = result.data
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun deleteVersion(versionId: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = deleteVersionUseCase(versionId)) {
                is Result.Success -> loadHistory(currentItemId)
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

    fun clearSelectedVersion() {
        selectedVersion = null
    }

    fun restoreVersion(itemId: String, versionNumber: Int, comment: String?) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = restoreVersionUseCase(itemId, versionNumber, comment)) {
                is Result.Success -> loadHistory(itemId)
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

    fun compareVersions(itemId: String, fromVersion: Int, toVersion: Int) {
        viewModelScope.launch {
            error = null
            when (val result = compareVersionsUseCase(itemId, fromVersion, toVersion)) {
                is Result.Success -> versionDiff = result.data
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
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
                is Result.Success -> loadHistory(itemId)
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

    fun clearDiff() {
        versionDiff = null
    }

    fun clearError() {
        error = null
    }
}
