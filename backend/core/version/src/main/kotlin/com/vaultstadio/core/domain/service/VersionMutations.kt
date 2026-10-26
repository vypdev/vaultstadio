package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.model.VersionRetentionPolicy
import com.vaultstadio.core.domain.repository.FileVersionRepository
import com.vaultstadio.domain.common.exception.InvalidOperationException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.version.repository.VersionContentReferenceRepository
import kotlinx.datetime.Clock
import java.util.UUID

/** Mutations accept only the canonical item authorized by FileVersionService. */
internal class VersionMutations(
    private val versions: FileVersionRepository,
    private val reads: VersionReads,
    private val storage: StorageBackend,
    private val contentReferences: VersionContentReferenceRepository,
) {
    suspend fun create(
        item: StorageItem,
        input: CreateVersionInput,
        actorId: String,
        restoredFrom: Int? = null,
    ): Either<StorageException, FileVersion> {
        if (item.type !=
            ItemType.FILE
        ) {
            return InvalidOperationException("createVersion", "Versioning only applies to files").left()
        }
        return versions.getNextVersionNumber(item.id).flatMap { number ->
            markPreviousLatest(item).flatMap {
                val row = FileVersion(
                    id = UUID.randomUUID().toString(), itemId = item.id, versionNumber = number,
                    size = input.size, checksum = input.checksum, storageKey = input.storageKey,
                    createdBy = actorId, createdAt = Clock.System.now(), comment = input.comment,
                    isLatest = true, restoredFrom = restoredFrom,
                )
                versions.create(row).flatMap { reads.bind(item, it, number = number, id = row.id) }
            }
        }
    }

    private suspend fun markPreviousLatest(item: StorageItem): Either<StorageException, Unit> =
        versions.findLatest(item.id).flatMap { latest ->
            if (latest == null) {
                Unit.right()
            } else {
                reads.bind(item, latest).flatMap { bound -> versions.update(bound.copy(isLatest = false)).map { Unit } }
            }
        }

    suspend fun restore(
        item: StorageItem,
        input: RestoreVersionInput,
        actorId: String,
    ): Either<StorageException, FileVersion> =
        reads.number(item, input.versionNumber).flatMap { old ->
            if (old.isLatest) {
                InvalidOperationException("restoreVersion", "Cannot restore the current version").left()
            } else {
                create(
                    item,
                    CreateVersionInput(
                        item.id,
                        old.size,
                        old.checksum,
                        old.storageKey,
                        input.comment ?: "Restored from version ${input.versionNumber}",
                    ),
                    actorId,
                    restoredFrom = input.versionNumber,
                )
            }
        }

    suspend fun delete(item: StorageItem, id: String): Either<StorageException, Unit> =
        versions.findById(id).flatMap { reads.bind(item, it, id = id) }.flatMap { row ->
            if (row.isLatest) {
                InvalidOperationException("deleteVersion", "Cannot delete the current version").left()
            } else {
                deleteUnreferencedContent(row).flatMap { versions.delete(id) }
            }
        }

    suspend fun retention(
        item: StorageItem,
        policy: VersionRetentionPolicy,
    ): Either<StorageException, List<String>> =
        reads.list(item, Int.MAX_VALUE, 0).flatMap { rows ->
            versions.applyRetentionPolicy(item.id, policy).flatMap { ids ->
                if (ids.any { id -> rows.none { it.id == id } }) scopeMismatch().left() else ids.right()
            }
        }

    suspend fun cleanup(item: StorageItem): Either<StorageException, Int> =
        reads.list(item, Int.MAX_VALUE, 0).flatMap { rows ->
            unreferencedRows(rows).flatMap deletion@{ removable ->
                for (row in removable) {
                    val deleted = storage.delete(row.storageKey)
                    if (deleted is Either.Left) return@deletion deleted
                }
                versions.deleteAllForItem(item.id)
            }
        }

    private suspend fun deleteUnreferencedContent(row: FileVersion): Either<StorageException, Unit> =
        unreferencedRows(listOf(row)).flatMap { removable ->
            if (removable.isEmpty()) Unit.right() else storage.delete(row.storageKey)
        }

    private suspend fun unreferencedRows(rows: List<FileVersion>): Either<StorageException, List<FileVersion>> {
        val removable = mutableListOf<FileVersion>()
        for (row in rows) {
            when (val result = contentReferences.hasOtherReferences(row.storageKey, row.id)) {
                is Either.Left -> return result
                is Either.Right -> if (!result.value) removable.add(row)
            }
        }
        return removable.right()
    }
}
