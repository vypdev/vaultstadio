package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.service.ArchiveWriter
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.ConcurrentModificationException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.ArchiveLimits

interface DownloadArchiveUseCase {
    suspend operator fun invoke(itemIds: List<String>, userId: String): Either<StorageException, ByteArray>
}

class DownloadArchiveUseCaseImpl(
    private val storage: StorageService,
    private val writer: ArchiveWriter,
    private val limits: ArchiveLimits = ArchiveLimits(),
) : DownloadArchiveUseCase {
    override suspend fun invoke(itemIds: List<String>, userId: String): Either<StorageException, ByteArray> {
        val entries = when (val selected = ArchiveSelection(storage, userId, limits).collect(itemIds)) {
            is Either.Left -> return selected
            is Either.Right -> selected.value
        }
        return writer.write(entries) { entry ->
            when (val result = storage.downloadFile(entry.item.id, userId)) {
                is Either.Left -> result
                is Either.Right -> {
                    val (current, stream) = result.value
                    if (current.ownerId != entry.item.ownerId ||
                        current.version != entry.item.version ||
                        current.name != entry.item.name ||
                        current.parentId != entry.item.parentId ||
                        current.storageKey != entry.item.storageKey ||
                        current.size != entry.item.size ||
                        current.checksum != entry.item.checksum
                    ) {
                        stream.close()
                        ConcurrentModificationException(current.id, entry.item.version, current.version).left()
                    } else {
                        stream.right()
                    }
                }
            }
        }
    }
}
