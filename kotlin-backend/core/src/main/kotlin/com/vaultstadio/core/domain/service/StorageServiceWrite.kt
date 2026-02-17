/**
 * Internal storage write operations used by StorageService.
 * Extracted to keep StorageService under the line limit.
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.event.FileEvent
import com.vaultstadio.core.domain.event.FolderEvent
import com.vaultstadio.core.domain.model.ItemType
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.model.Visibility
import com.vaultstadio.core.domain.repository.StorageItemRepository
import com.vaultstadio.core.exception.ItemNotFoundException
import com.vaultstadio.core.exception.StorageException
import com.vaultstadio.core.exception.ValidationException
import kotlinx.datetime.Clock
import java.util.UUID

internal suspend fun resolveUniqueNameAndPath(
    repository: StorageItemRepository,
    requestedName: String,
    parentPath: String,
    ownerId: String,
): Either<StorageException, Pair<String, String>> {
    fun candidateName(base: String, counter: Int): String {
        val lastDot = base.lastIndexOf('.')
        return if (lastDot > 0) {
            "${base.substring(0, lastDot)}_$counter.${base.substring(lastDot + 1)}"
        } else {
            "${base}_$counter"
        }
    }
    var counter = 1
    var name = requestedName
    var path = if (parentPath.isEmpty()) "/$name" else "$parentPath/$name"
    while (true) {
        when (val exists = repository.existsByPath(path, ownerId)) {
            is Either.Left -> return exists
            is Either.Right -> if (!exists.value) return Pair(name, path).right()
        }
        counter++
        name = candidateName(requestedName, counter)
        path = if (parentPath.isEmpty()) "/$name" else "$parentPath/$name"
    }
}

internal suspend fun createFolderInternal(
    repository: StorageItemRepository,
    eventBus: EventBus,
    input: CreateFolderInput,
): Either<StorageException, StorageItem> {
    if (input.name.isBlank()) {
        return ValidationException("Folder name cannot be empty").left()
    }

    val parentPath = if (input.parentId != null) {
        when (val result = repository.findById(input.parentId)) {
            is Either.Left -> return result
            is Either.Right -> {
                val parent = result.value
                    ?: return ItemNotFoundException("Parent folder not found").left()
                if (parent.type != ItemType.FOLDER) {
                    return ValidationException("Parent must be a folder").left()
                }
                parent.path
            }
        }
    } else {
        ""
    }

    val newPath = if (parentPath.isEmpty()) "/${input.name}" else "$parentPath/${input.name}"

    when (val result = repository.existsByPath(newPath, input.ownerId)) {
        is Either.Left -> return result
        is Either.Right -> {
            if (result.value) {
                return ValidationException("Folder already exists at this location").left()
            }
        }
    }

    val now = Clock.System.now()
    val folder = StorageItem(
        id = UUID.randomUUID().toString(),
        name = input.name,
        path = newPath,
        type = ItemType.FOLDER,
        parentId = input.parentId,
        ownerId = input.ownerId,
        size = 0,
        mimeType = null,
        checksum = null,
        storageKey = null,
        visibility = Visibility.PRIVATE,
        isTrashed = false,
        isStarred = false,
        createdAt = now,
        updatedAt = now,
        trashedAt = null,
        version = 1,
    )

    return repository.create(folder).also { result ->
        if (result.isRight()) {
            val item = (result as Either.Right).value
            eventBus.publish(FolderEvent.Created(userId = input.ownerId, folder = item))
        }
    }
}

internal suspend fun getOrCreateFolderInternal(
    repository: StorageItemRepository,
    eventBus: EventBus,
    name: String,
    parentId: String?,
    ownerId: String,
): Either<StorageException, GetOrCreateFolderResult> {
    if (name.isBlank()) {
        return ValidationException("Folder name cannot be empty").left()
    }

    val parentPath = if (parentId != null) {
        when (val result = repository.findById(parentId)) {
            is Either.Left -> return result
            is Either.Right -> {
                val parent = result.value
                    ?: return ItemNotFoundException("Parent folder not found").left()
                if (parent.type != ItemType.FOLDER) {
                    return ValidationException("Parent must be a folder").left()
                }
                parent.path
            }
        }
    } else {
        ""
    }

    val targetPath = if (parentPath.isEmpty()) "/$name" else "$parentPath/$name"

    when (val result = repository.findByPath(targetPath, ownerId)) {
        is Either.Left -> return result
        is Either.Right -> {
            val existing = result.value
            if (existing != null && existing.type == ItemType.FOLDER) {
                return GetOrCreateFolderResult(folder = existing, created = false).right()
            }
        }
    }

    val now = Clock.System.now()
    val folder = StorageItem(
        id = UUID.randomUUID().toString(),
        name = name,
        path = targetPath,
        type = ItemType.FOLDER,
        parentId = parentId,
        ownerId = ownerId,
        size = 0,
        mimeType = null,
        checksum = null,
        storageKey = null,
        visibility = Visibility.PRIVATE,
        isTrashed = false,
        isStarred = false,
        createdAt = now,
        updatedAt = now,
        trashedAt = null,
        version = 1,
    )

    return repository.create(folder).fold(
        { error -> error.left() },
        { created ->
            eventBus.publish(FolderEvent.Created(userId = ownerId, folder = created))
            GetOrCreateFolderResult(folder = created, created = true).right()
        },
    )
}

internal suspend fun uploadFileInternal(
    repository: StorageItemRepository,
    storageBackend: StorageBackend,
    eventBus: EventBus,
    input: UploadFileInput,
): Either<StorageException, StorageItem> {
    if (input.name.isBlank()) {
        return ValidationException("File name cannot be empty").left()
    }

    val parentPath = if (input.parentId != null) {
        when (val result = repository.findById(input.parentId)) {
            is Either.Left -> return result
            is Either.Right -> {
                val parent = result.value
                    ?: return ItemNotFoundException("Parent folder not found").left()
                parent.path
            }
        }
    } else {
        ""
    }

    val (finalName, newPath) = when (
        val resolved = resolveUniqueNameAndPath(
            repository,
            input.name,
            parentPath,
            input.ownerId,
        )
    ) {
        is Either.Left -> return resolved
        is Either.Right -> resolved.value
    }

    val storageKey = when (val result = storageBackend.store(input.inputStream, input.size, input.mimeType)) {
        is Either.Left -> return result
        is Either.Right -> result.value
    }

    val now = Clock.System.now()
    val file = StorageItem(
        id = UUID.randomUUID().toString(),
        name = finalName,
        path = newPath,
        type = ItemType.FILE,
        parentId = input.parentId,
        ownerId = input.ownerId,
        size = input.size,
        mimeType = input.mimeType,
        checksum = null,
        storageKey = storageKey,
        visibility = Visibility.PRIVATE,
        isTrashed = false,
        isStarred = false,
        createdAt = now,
        updatedAt = now,
        trashedAt = null,
        version = 1,
    )

    return repository.create(file).also { result ->
        if (result.isRight()) {
            val item = (result as Either.Right).value
            eventBus.publish(FileEvent.Uploaded(userId = input.ownerId, item = item))
        }
    }
}
