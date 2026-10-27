package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.exception.ValidationException
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.common.pagination.SortOrder
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.model.StorageSelection
import com.vaultstadio.domain.storage.repository.SortField
import com.vaultstadio.domain.storage.repository.StorageItemQuery
import com.vaultstadio.domain.storage.repository.StorageItemRepository

internal suspend fun listFolderSelection(
    repository: StorageItemRepository,
    folderId: String?,
    actorId: String,
    query: StorageItemQuery,
): Either<StorageException, PagedResult<StorageItem>> = either {
    val selection = checkedSelection(query.limit, query.offset).bind()
    if (folderId != null) {
        val parent = getItemInternal(repository, folderId, actorId).bind()
        ensure(parent.type == ItemType.FOLDER) { ValidationException(field = "folder", message = "Not a folder") }
    }
    repository.query(
        query.copy(
            parentId = folderId,
            ownerId = actorId,
            filterByParent = true,
            limit = selection.limit,
            offset = selection.offset,
        ),
    ).bind()
}

internal suspend fun recentSelection(
    repository: StorageItemRepository,
    actorId: String,
    limit: Int,
): Either<StorageException, List<StorageItem>> = either {
    val selection = checkedSelection(limit, 0).bind()
    repository.query(
        StorageItemQuery(
            ownerId = actorId,
            isTrashed = false,
            sortField = SortField.UPDATED_AT,
            sortOrder = SortOrder.DESC,
            limit = selection.limit,
        ),
    ).bind().items
}

internal suspend fun searchSelection(
    repository: StorageItemRepository,
    query: String,
    actorId: String,
    limit: Int,
    offset: Int,
): Either<StorageException, PagedResult<StorageItem>> = either {
    val selection = checkedSelection(limit, offset).bind()
    repository.query(
        StorageItemQuery(
            ownerId = actorId,
            searchQuery = query,
            isTrashed = false,
            limit = selection.limit,
            offset = selection.offset,
        ),
    ).bind()
}

private fun checkedSelection(limit: Int, offset: Int): Either<StorageException, StorageSelection> =
    StorageSelection.create(limit, offset).mapLeft { ValidationException(field = it.parameter.name.lowercase()) }
