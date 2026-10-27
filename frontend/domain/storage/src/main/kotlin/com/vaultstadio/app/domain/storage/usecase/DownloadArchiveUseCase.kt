package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result

interface DownloadArchiveUseCase {
    suspend operator fun invoke(itemIds: List<String>): Result<ByteArray>
}
