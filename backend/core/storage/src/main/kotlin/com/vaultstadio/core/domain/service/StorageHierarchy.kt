package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.exception.ValidationException
import com.vaultstadio.domain.storage.model.StorageAccessPolicy
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.repository.StorageItemRepository

private const val MAX_ANCESTORS = 128

/** Load the whole owned chain before exposing metadata or performing effects. */
internal suspend fun ownedAncestors(
    repository: StorageItemRepository,
    parentId: String?,
    actorId: String,
    active: Boolean,
    forbiddenAncestorId: String? = null,
): Either<StorageException, List<StorageItem>> {
    val ancestors = mutableListOf<StorageItem>()
    val visited = mutableSetOf<String>()
    var currentId = parentId
    while (currentId != null) {
        if (currentId == forbiddenAncestorId || !visited.add(currentId) || ancestors.size >= MAX_ANCESTORS) {
            return ValidationException(message = "Invalid or too deep folder hierarchy").left()
        }
        val item = when (val result = getItemInternal(repository, currentId, actorId)) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }
        when (val result = StorageAccessPolicy.requireFolder(item, actorId, active)) {
            is Either.Left -> return result
            is Either.Right -> ancestors.add(result.value)
        }
        currentId = item.parentId
    }
    return ancestors.right()
}

internal suspend fun ownedParentPath(
    repository: StorageItemRepository,
    parentId: String?,
    actorId: String,
    forbiddenAncestorId: String? = null,
): Either<StorageException, String> =
    ownedAncestors(repository, parentId, actorId, active = true, forbiddenAncestorId = forbiddenAncestorId)
        .map { it.firstOrNull()?.path.orEmpty() }

internal suspend fun ownedBreadcrumbs(
    repository: StorageItemRepository,
    itemId: String,
    actorId: String,
): Either<StorageException, List<StorageItem>> =
    getItemInternal(repository, itemId, actorId).fold(
        { it.left() },
        { item -> ownedAncestors(repository, item.parentId, actorId, active = false).map { it.reversed() } },
    )
