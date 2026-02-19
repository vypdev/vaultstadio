package com.vaultstadio.app.feature.sharedwithme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.domain.config.usecase.GetStorageUrlsUseCase
import com.vaultstadio.app.domain.share.usecase.DeleteShareUseCase
import com.vaultstadio.app.domain.share.usecase.GetSharedWithMeUseCase
import com.vaultstadio.app.domain.storage.usecase.GetItemUseCase
import com.vaultstadio.app.ui.screens.SharedWithMeItem
import kotlinx.coroutines.launch
/**
 * ViewModel for items shared with the current user.
 */
class SharedWithMeViewModel(
    private val getSharedWithMeUseCase: GetSharedWithMeUseCase,
    private val getItemUseCase: GetItemUseCase,
    private val deleteShareUseCase: DeleteShareUseCase,
    private val getStorageUrlsUseCase: GetStorageUrlsUseCase,
) : ViewModel() {

    var sharedItems by mutableStateOf<List<SharedWithMeItem>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var selectedItem by mutableStateOf<StorageItem?>(null)
        private set
    var downloadUrl by mutableStateOf<String?>(null)
        private set

    init {
        loadSharedItems()
    }

    fun loadSharedItems() {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val sharesResult = getSharedWithMeUseCase()) {
                is Result.Success -> {
                    // For each share, fetch the item details
                    val items = mutableListOf<SharedWithMeItem>()
                    for (share in sharesResult.data) {
                        when (val itemResult = getItemUseCase(share.itemId)) {
                            is Result.Success -> {
                                val item = itemResult.data
                                items.add(
                                    SharedWithMeItem(
                                        item = item,
                                        sharedBy = share.createdBy,
                                        sharedByEmail = "", // Email not stored in share
                                        sharedAt = share.createdAt,
                                        permissions = listOf("read", "download"),
                                    ),
                                )
                            }
                            else -> {
                                // Skip items that can't be fetched
                            }
                        }
                    }
                    sharedItems = items
                }
                is Result.Error -> error = sharesResult.message
                is Result.NetworkError -> error = sharesResult.message
            }
            isLoading = false
        }
    }

    fun onItemClick(item: StorageItem) {
        selectedItem = item
    }

    fun clearSelectedItem() {
        selectedItem = null
    }

    fun downloadItem(item: StorageItem) {
        downloadUrl = getStorageUrlsUseCase.downloadUrl(item.id)
    }

    fun clearDownloadUrl() {
        downloadUrl = null
    }

    fun removeShare(itemId: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = deleteShareUseCase(itemId)) {
                is Result.Success -> loadSharedItems()
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

    fun clearError() {
        error = null
    }
}
