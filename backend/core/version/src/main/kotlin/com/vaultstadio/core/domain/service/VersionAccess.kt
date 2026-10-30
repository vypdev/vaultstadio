package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.StorageAccessPolicy
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.repository.StorageItemRepository
import com.vaultstadio.domain.version.repository.VersionItemScopeRepository

/** No transport, SQL, role or visibility shortcut participates in the owner decision. */
internal class VersionAccess(
    private val items: StorageItemRepository,
    private val scopes: VersionItemScopeRepository,
) {
    suspend fun item(itemId: String, actorId: String): Either<StorageException, StorageItem> {
        if (actorId.isBlank()) return AuthorizationException().left()
        return items.findById(itemId).flatMap { item ->
            when {
                item == null -> ItemNotFoundException().left()
                item.id != itemId -> scopeMismatch().left()
                else -> StorageAccessPolicy.requireOwner(item, actorId)
            }
        }
    }

    suspend fun version(versionId: String, actorId: String): Either<StorageException, StorageItem> {
        if (actorId.isBlank()) return AuthorizationException().left()
        return scopes.findItemId(versionId).flatMap { itemId ->
            if (itemId == null) {
                ItemNotFoundException(message = "Version not found").left()
            } else {
                item(itemId, actorId)
            }
        }
    }
}

internal fun scopeMismatch() = DatabaseException("Version repository returned an inconsistent scope")
