package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.model.FileVersionHistory
import com.vaultstadio.core.domain.repository.FileVersionRepository
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.StorageItem

/** Bind adapter results to the already authorized canonical item before disclosure. */
internal class VersionReads(private val versions: FileVersionRepository) {
    fun bind(
        item: StorageItem,
        version: FileVersion?,
        number: Int? = null,
        id: String? = null,
    ): Either<StorageException, FileVersion> = when {
        version == null -> ItemNotFoundException(message = "Version not found").left()
        version.itemId != item.id -> scopeMismatch().left()
        number != null && version.versionNumber != number -> scopeMismatch().left()
        id != null && version.id != id -> scopeMismatch().left()
        else -> version.right()
    }

    suspend fun number(item: StorageItem, number: Int): Either<StorageException, FileVersion> =
        versions.findByItemAndVersion(item.id, number).flatMap { bind(item, it, number = number) }

    suspend fun latest(item: StorageItem): Either<StorageException, FileVersion> =
        versions.findLatest(item.id).flatMap { bind(item, it) }.flatMap {
            if (it.isLatest) it.right() else scopeMismatch().left()
        }

    suspend fun list(
        item: StorageItem,
        limit: Int,
        offset: Int,
    ): Either<StorageException, List<FileVersion>> =
        versions.listVersions(item.id, limit, offset).flatMap { rows ->
            if (rows.any { it.itemId != item.id }) scopeMismatch().left() else rows.right()
        }

    suspend fun history(item: StorageItem): Either<StorageException, FileVersionHistory> =
        versions.getHistory(item.id).flatMap { history ->
            if (history.item.id != item.id || history.versions.any { it.itemId != item.id }) {
                scopeMismatch().left()
            } else {
                // Name/owner/visibility always come from the current authorized item.
                history.copy(item = item).right()
            }
        }
}
