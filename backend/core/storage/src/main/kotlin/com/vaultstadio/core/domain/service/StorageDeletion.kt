package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.event.FileEvent
import com.vaultstadio.domain.common.exception.InvalidOperationException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.repository.StorageItemQuery
import com.vaultstadio.domain.storage.repository.StorageItemRepository

internal suspend fun deleteItemInternal(
    repository: StorageItemRepository,
    storageBackend: StorageBackend,
    eventBus: EventBus,
    itemId: String,
    userId: String,
): Either<StorageException, Unit> = either {
    val item = getItemInternal(repository, itemId, userId).bind()
    val childQuery = StorageItemQuery(parentId = itemId, filterByParent = true, limit = 1)
    val children = repository.query(childQuery).bind()
    ensure(children.items.isEmpty()) {
        InvalidOperationException(
            operation = "delete-nonempty-folder",
            message = "Delete the folder contents before permanently deleting this folder",
        )
    }
    val storageKey = item.storageKey
    if (item.type == ItemType.FILE && storageKey != null) {
        storageBackend.delete(storageKey).bind()
    }
    repository.delete(itemId).bind()
    eventBus.publish(FileEvent.Deleted(userId = userId, item = item, permanent = true))
}
