package com.vaultstadio.domain.storage.model

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.exception.ValidationException

/** Ordinary storage access is owner-scoped. Share grants are validated separately. */
object StorageAccessPolicy {
    fun requireOwner(item: StorageItem, actorId: String): Either<StorageException, StorageItem> =
        if (item.ownerId == actorId) item.right() else AuthorizationException().left()

    fun requireFolder(
        item: StorageItem,
        actorId: String,
        active: Boolean,
    ): Either<StorageException, StorageItem> = requireOwner(item, actorId).flatMap {
        when {
            it.type != ItemType.FOLDER -> ValidationException(message = "Parent must be a folder").left()
            active && it.isTrashed -> ValidationException(message = "Parent folder is trashed").left()
            else -> it.right()
        }
    }
}
