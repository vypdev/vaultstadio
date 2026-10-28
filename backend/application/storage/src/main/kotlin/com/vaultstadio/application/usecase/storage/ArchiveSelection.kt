package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.FileSizeLimitException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.exception.ValidationException
import com.vaultstadio.domain.storage.model.ArchiveEntry
import com.vaultstadio.domain.storage.model.ArchiveLimits
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.model.isSafeArchiveSegment
import com.vaultstadio.domain.storage.repository.StorageItemQuery
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** One invocation owns a bounded metadata snapshot. No content is opened during selection. */
internal class ArchiveSelection(
    private val storage: StorageService,
    private val userId: String,
    private val limits: ArchiveLimits,
) {
    private val entries = mutableListOf<ArchiveEntry>()
    private val ids = mutableSetOf<String>()
    private val paths = mutableSetOf<String>()
    private var bytes = 0L

    suspend fun collect(requested: List<String>): Either<StorageException, List<ArchiveEntry>> {
        if (requested.isEmpty() ||
            requested.size > limits.entries ||
            requested.any { it.isBlank() } ||
            requested.distinct().size != requested.size
        ) {
            return invalid("Select unique items within the archive limit")
        }
        for (id in requested) {
            val item = when (val result = storage.getItem(id, userId)) {
                is Either.Left -> return result
                is Either.Right -> result.value
            }
            when (val result = visit(item, item.name, 1)) {
                is Either.Left -> return result
                is Either.Right -> Unit
            }
        }
        return entries.toList().right()
    }

    private suspend fun visit(item: StorageItem, path: String, depth: Int): Either<StorageException, Unit> {
        currentCoroutineContext().ensureActive()
        if (!isSafeArchiveSegment(item.name) || !ids.add(item.id) || !paths.add(path)) {
            return invalid("Archive selection has an unsafe or ambiguous path")
        }
        if (entries.size >= limits.entries || (item.type == ItemType.FOLDER && depth > limits.depth)) {
            return invalid("Archive entry or folder depth limit exceeded")
        }
        if (item.type == ItemType.FILE && (item.size < 0 || item.size > limits.inputBytes - bytes)) {
            return FileSizeLimitException(item.size, limits.inputBytes, "Archive content exceeds its limit").left()
        }
        if (item.type == ItemType.FILE) bytes += item.size
        entries += ArchiveEntry(path, item)
        if (item.type == ItemType.FILE) return Unit.right()
        var offset = 0
        do {
            val page = when (
                val result = storage.listFolder(
                    item.id,
                    userId,
                    StorageItemQuery(limit = 256, offset = offset),
                )
            ) {
                is Either.Left -> return result
                is Either.Right -> result.value
            }
            if (page.items.isEmpty() && page.hasMore) return invalid("Archive listing did not advance")
            for (child in page.items) {
                val owned = when (val result = storage.getItem(child.id, userId)) {
                    is Either.Left -> return result
                    is Either.Right -> result.value
                }
                if (owned.parentId != item.id) return invalid("Archive folder changed during selection")
                when (val result = visit(owned, "$path/${owned.name}", depth + 1)) {
                    is Either.Left -> return result
                    is Either.Right -> Unit
                }
            }
            offset += page.items.size
        } while (page.hasMore)
        return Unit.right()
    }

    private fun invalid(message: String) = ValidationException(message).left()
}
