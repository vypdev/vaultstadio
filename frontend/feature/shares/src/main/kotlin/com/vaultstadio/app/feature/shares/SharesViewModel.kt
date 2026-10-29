package com.vaultstadio.app.feature.shares

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.domain.config.usecase.GetShareUrlUseCase
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.share.ClipboardWriteOutcome
import com.vaultstadio.app.domain.share.ShareLinkClipboard
import com.vaultstadio.app.domain.share.model.ShareLink
import com.vaultstadio.app.domain.share.usecase.DeleteShareUseCase
import com.vaultstadio.app.domain.share.usecase.GetMySharesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class SharesViewModel(
    private val getMySharesUseCase: GetMySharesUseCase,
    private val deleteShareUseCase: DeleteShareUseCase,
    private val getShareUrlUseCase: GetShareUrlUseCase,
    private val clipboard: ShareLinkClipboard,
) : ViewModel() {

    var shares by mutableStateOf<List<ShareLink>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var clipboardLink by mutableStateOf<String?>(null)
        private set

    var isCopyingLink by mutableStateOf(false)
        private set
    var copyError by mutableStateOf<String?>(null)
        private set
    private var copyJob: Job? = null
    private var copyGeneration = 0L

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

    // Unexpected output-port failure still needs safe feedback; cancellation escapes.
    @Suppress("TooGenericExceptionCaught")
    fun copyLink(share: ShareLink) {
        if (isCopyingLink || !viewModelScope.isActive) return
        val generation = ++copyGeneration
        clipboardLink = null
        copyError = null
        isCopyingLink = true
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                val link = getShareUrlUseCase(share.token)
                val outcome = withTimeoutOrNull(COPY_DEADLINE_MS) { clipboard.write(link) }
                currentCoroutineContext().ensureActive()
                if (generation == copyGeneration) {
                    if (outcome == ClipboardWriteOutcome.COPIED) {
                        clipboardLink = link
                    } else {
                        copyError = copyFailureMessage(outcome)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (generation == copyGeneration) copyError = copyFailureMessage(ClipboardWriteOutcome.FAILED)
            } finally {
                if (generation == copyGeneration) {
                    isCopyingLink = false
                    copyJob = null
                }
            }
        }
        copyJob = job
        job.start()
    }

    fun cancelCopy() {
        ++copyGeneration
        copyJob?.cancel()
        copyJob = null
        isCopyingLink = false
        clipboardLink = null
        copyError = null
    }

    fun clearCopyError() {
        copyError = null
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

    private fun copyFailureMessage(outcome: ClipboardWriteOutcome?): String = when (outcome) {
        ClipboardWriteOutcome.DENIED -> "Clipboard access was denied. Activate this window and try again."
        ClipboardWriteOutcome.UNAVAILABLE -> "Clipboard access is unavailable in this client."
        null -> "The clipboard did not respond. Try again."
        else -> "The link could not be copied. Try again."
    }

    private companion object {
        const val COPY_DEADLINE_MS = 5000L
    }
}
