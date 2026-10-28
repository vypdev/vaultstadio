package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.storage.transfer.DownloadPayload
import com.vaultstadio.app.domain.storage.transfer.DownloadSaver
import com.vaultstadio.app.domain.storage.transfer.SaveOutcome

actual fun createDownloadSaver(): DownloadSaver = object : DownloadSaver {
    override suspend fun save(payload: DownloadPayload) = SaveOutcome.UNSUPPORTED
}
