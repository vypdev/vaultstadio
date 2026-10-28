package com.vaultstadio.app.feature.files.transfer

import com.vaultstadio.app.domain.storage.transfer.SaveOutcome

enum class DownloadProblem {
    AUTHENTICATION,
    ACCESS_DENIED,
    NOT_FOUND,
    TOO_LARGE,
    INVALID_SELECTION,
    INVALID_NAME,
    CONTENT_CHANGED,
    NETWORK,
    UNAVAILABLE,
    SAVE_FAILED,
    UNSUPPORTED,
}

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Running(val name: String, val saving: Boolean = false) : DownloadState
    data class Finished(
        val name: String,
        val outcome: SaveOutcome,
        val destinationMayHaveContent: Boolean = false,
    ) : DownloadState
    data class Failed(val problem: DownloadProblem) : DownloadState
}

internal class DownloadRefused(val problem: DownloadProblem) : Exception()
