/**
 * Internal storage mutation operations used by StorageService.
 * Extracted to keep StorageService under the line limit.
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.event.FileEvent
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.repository.StorageItemRepository
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.exception.ValidationException
import kotlinx.datetime.Clock
import java.util.UUID

internal suspend fun renameItemInternal(
    repository: StorageItemRepository,
    getItem: suspend (String, String) -> Either<StorageException, StorageItem>,
    itemId: String,
    newName: String,
    userId: String,
): Either<StorageException, StorageItem> {
    if (newName.isBlank()) {
        return ValidationException("Name cannot be empty").left()
    }

    val item = when (val result = getItem(itemId, userId)) {
        is Either.Left -> return result
        is Either.Right -> result.value
    }

    val newPath = item.path.substringBeforeLast("/") + "/" + newName
    val updatedItem = item.copy(
        name = newName,
        path = newPath,
        updatedAt = Clock.System.now(),
        version = item.version + 1,
    )

    return repository.update(updatedItem)
}

internal suspend fun moveItemInternal(
    repository: StorageItemRepository,
    getItem: suspend (String, String) -> Either<StorageException, StorageItem>,
    input: MoveItemInput,
): Either<StorageException, StorageItem> {
    val item = when (val result = getItem(input.itemId, input.userId)) {
        is Either.Left -> return result
        is Either.Right -> result.value
    }

    val newParentPath = if (input.newParentId != null) {
        when (val result = getItem(input.newParentId, input.userId)) {
            is Either.Left -> return result
            is Either.Right -> {
                if (result.value.type != ItemType.FOLDER) {
                    return ValidationException("Destination must be a folder").left()
                }
                result.value.path
            }
        }
    } else {
        ""
    }

    val newName = input.newName ?: item.name
    val newPath = if (newParentPath.isEmpty()) "/$newName" else "$newParentPath/$newName"

    val updatedItem = item.copy(
        name = newName,
        path = newPath,
        parentId = input.newParentId,
        updatedAt = Clock.System.now(),
        version = item.version + 1,
    )

    return repository.update(updatedItem)
}

internal suspend fun copyItemInternal(
    repository: StorageItemRepository,
    storageBackend: StorageBackend,
    getItem: suspend (String, String) -> Either<StorageException, StorageItem>,
    input: CopyItemInput,
): Either<StorageException, StorageItem> {
    val item = when (val result = getItem(input.itemId, input.userId)) {
        is Either.Left -> return result
        is Either.Right -> result.value
    }

    val destParentPath = if (input.destinationParentId != null) {
        when (val result = getItem(input.destinationParentId, input.userId)) {
            is Either.Left -> return result
            is Either.Right -> {
                if (result.value.type != ItemType.FOLDER) {
                    return ValidationException("Destination must be a folder").left()
                }
                result.value.path
            }
        }
    } else {
        ""
    }

    val newName = input.newName ?: "Copy of ${item.name}"
    val newPath = if (destParentPath.isEmpty()) "/$newName" else "$destParentPath/$newName"

    val itemStorageKey = item.storageKey
    val newStorageKey = if (item.type == ItemType.FILE && itemStorageKey != null) {
        when (val result = storageBackend.copy(itemStorageKey)) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }
    } else {
        null
    }

    val now = Clock.System.now()
    val newItem = item.copy(
        id = UUID.randomUUID().toString(),
        name = newName,
        path = newPath,
        parentId = input.destinationParentId,
        storageKey = newStorageKey,
        createdAt = now,
        updatedAt = now,
        version = 1,
    )

    return repository.create(newItem)
}

internal suspend fun toggleStarInternal(
    repository: StorageItemRepository,
    getItem: suspend (String, String) -> Either<StorageException, StorageItem>,
    itemId: String,
    userId: String,
): Either<StorageException, StorageItem> {
    val item = when (val result = getItem(itemId, userId)) {
        is Either.Left -> return result
        is Either.Right -> result.value
    }

    val updatedItem = item.copy(
        isStarred = !item.isStarred,
        updatedAt = Clock.System.now(),
        version = item.version + 1,
    )

    return repository.update(updatedItem)
}

internal suspend fun setStarInternal(
    repository: StorageItemRepository,
    getItem: suspend (String, String) -> Either<StorageException, StorageItem>,
    itemId: String,
    userId: String,
    starred: Boolean,
): Either<StorageException, StorageItem> {
    val item = when (val result = getItem(itemId, userId)) {
        is Either.Left -> return result
        is Either.Right -> result.value
    }

    if (item.isStarred == starred) {
        return item.right()
    }

    val updatedItem = item.copy(
        isStarred = starred,
        updatedAt = Clock.System.now(),
        version = item.version + 1,
    )

    return repository.update(updatedItem)
}

internal suspend fun trashItemInternal(
    repository: StorageItemRepository,
    eventBus: EventBus,
    getItem: suspend (String, String) -> Either<StorageException, StorageItem>,
    itemId: String,
    userId: String,
): Either<StorageException, StorageItem> {
    val item = when (val result = getItem(itemId, userId)) {
        is Either.Left -> return result
        is Either.Right -> result.value
    }

    val now = Clock.System.now()
    val trashedItem = item.copy(
        isTrashed = true,
        trashedAt = now,
        updatedAt = now,
        version = item.version + 1,
    )

    return repository.update(trashedItem).also { result ->
        if (result.isRight()) {
            val updated = (result as Either.Right).value
            eventBus.publish(FileEvent.Deleted(userId = userId, item = updated, permanent = false))
        }
    }
}

internal suspend fun restoreItemInternal(
    repository: StorageItemRepository,
    eventBus: EventBus,
    getItem: suspend (String, String) -> Either<StorageException, StorageItem>,
    itemId: String,
    userId: String,
): Either<StorageException, StorageItem> {
    val item = when (val result = getItem(itemId, userId)) {
        is Either.Left -> return result
        is Either.Right -> result.value
    }

    val restoredItem = item.copy(
        isTrashed = false,
        trashedAt = null,
        updatedAt = Clock.System.now(),
        version = item.version + 1,
    )

    return repository.update(restoredItem).also { result ->
        if (result.isRight()) {
            val updated = (result as Either.Right).value
            eventBus.publish(FileEvent.Restored(userId = userId, item = updated))
        }
    }
}

internal suspend fun deleteItemInternal(
    repository: StorageItemRepository,
    storageBackend: StorageBackend,
    eventBus: EventBus,
    getItem: suspend (String, String) -> Either<StorageException, StorageItem>,
    itemId: String,
    userId: String,
): Either<StorageException, Unit> {
    val item = when (val result = getItem(itemId, userId)) {
        is Either.Left -> return result
        is Either.Right -> result.value
    }

    val itemStorageKey = item.storageKey
    if (item.type == ItemType.FILE && itemStorageKey != null) {
        when (val result = storageBackend.delete(itemStorageKey)) {
            is Either.Left -> return result
            is Either.Right -> { /* continue */ }
        }
    }

    return repository.delete(itemId).also { result ->
        if (result.isRight()) {
            eventBus.publish(FileEvent.Deleted(userId = userId, item = item, permanent = true))
        }
    }
}
