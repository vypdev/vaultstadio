package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.raise.either
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.event.FileEvent
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.repository.StorageItemRepository

internal suspend fun deleteItemInternal(
    repository: StorageItemRepository,
    storageBackend: StorageBackend,
    eventBus: EventBus,
    itemId: String,
    userId: String,
): Either<StorageException, Unit> = either {
    getItemInternal(repository, itemId, userId).bind()
    val deleted = repository.deleteLeaf(itemId, userId) { lockedItem ->
        either {
            val storageKey = lockedItem.storageKey
            if (lockedItem.type == ItemType.FILE && storageKey != null) {
                storageBackend.delete(storageKey).bind()
            }
        }
    }.bind()
    eventBus.publish(FileEvent.Deleted(userId = userId, item = deleted, permanent = true))
}
