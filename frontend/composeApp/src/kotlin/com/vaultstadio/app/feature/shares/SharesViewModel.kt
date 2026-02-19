package com.vaultstadio.app.feature.shares

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.model.ShareLink
import com.vaultstadio.app.domain.usecase.config.GetShareUrlUseCase
import com.vaultstadio.app.domain.usecase.share.DeleteShareUseCase
import com.vaultstadio.app.domain.usecase.share.GetMySharesUseCase
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * ViewModel for managing user's shared links.
 */
@KoinViewModel
class SharesViewModel(
    private val getMySharesUseCase: GetMySharesUseCase,
    private val deleteShareUseCase: DeleteShareUseCase,
    private val getShareUrlUseCase: GetShareUrlUseCase,
) : ViewModel() {

    var shares by mutableStateOf<List<ShareLink>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var clipboardLink by mutableStateOf<String?>(null)
        private set

    init {
        loadShares()
    }

    fun loadShares() {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = getMySharesUseCase()) {
                is Result.Success -> shares = result.data
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
            isLoading = false
        }
    }

    fun copyLink(share: ShareLink) {
        clipboardLink = getShareUrlUseCase(share.token)
    }

    fun clearClipboardLink() {
        clipboardLink = null
    }

    fun deleteShare(share: ShareLink) {
        viewModelScope.launch {
            isLoading = true
            when (val result = deleteShareUseCase(share.id)) {
                is Result.Success -> loadShares()
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
