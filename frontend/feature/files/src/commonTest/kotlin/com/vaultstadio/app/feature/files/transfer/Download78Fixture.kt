package com.vaultstadio.app.feature.files.transfer

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.ItemType
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.domain.storage.model.Visibility
import com.vaultstadio.app.domain.storage.transfer.DownloadPayload
import com.vaultstadio.app.domain.storage.transfer.DownloadSaver
import com.vaultstadio.app.domain.storage.transfer.SaveOutcome
import com.vaultstadio.app.domain.storage.usecase.DownloadArchiveUseCase
import com.vaultstadio.app.domain.storage.usecase.DownloadFileUseCase
import com.vaultstadio.app.domain.storage.usecase.GetItemUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlin.time.Instant

internal class Download78Fixture(scope: CoroutineScope) {
    val bytes = byteArrayOf(0, -1, 127)
    val item = StorageItem(
        "file78", "Archivo ñ.bin", "/Archivo ñ.bin", ItemType.FILE, null, 3,
        "application/octet-stream", Visibility.PRIVATE, false, false,
        Instant.fromEpochMilliseconds(
            0,
        ),
        Instant.fromEpochMilliseconds(0),
    )
    var metadata: suspend (String) -> Result<StorageItem> = { Result.Success(item) }
    var read: suspend (String) -> Result<ByteArray> = { Result.Success(bytes) }
    var zip: suspend (List<String>) -> Result<ByteArray> = { Result.Success(bytes) }
    var save: suspend (DownloadPayload) -> SaveOutcome = { SaveOutcome.SAVED }
    val events = mutableListOf<String>()
    val archives = mutableListOf<List<String>>()
    val saves = mutableListOf<DownloadPayload>()
    var closed = 0
    val controller = FilesDownloadController(
        scope,
        object : GetItemUseCase {
            override suspend fun invoke(
                itemId: String,
            ): Result<StorageItem> {
                events += "metadata:$itemId"
                return metadata(itemId)
            }
        },
        object : DownloadFileUseCase {
            override suspend fun invoke(
                itemId: String,
            ): Result<ByteArray> {
                events += "bytes:$itemId"
                return read(itemId)
            }
            override fun getDownloadUrl(itemId: String): String = error("Never navigate to a protected URL")
        },
        object : DownloadArchiveUseCase {
            override suspend fun invoke(itemIds: List<String>): Result<ByteArray> {
                archives += itemIds
                events +=
                    "archive"
                return zip(itemIds)
            }
        },
        object : DownloadSaver {
            override suspend fun save(
                payload: DownloadPayload,
            ): SaveOutcome {
                events += "save"
                saves += payload
                return this@Download78Fixture.save(payload)
            }
            override fun close() {
                closed++
            }
        },
    )
}
internal suspend fun TestScope.withDownload78(block: suspend Download78Fixture.() -> Unit) {
    val fixture = Download78Fixture(backgroundScope)
    try {
        fixture.block()
    } finally {
        fixture.controller.close()
    }
}
