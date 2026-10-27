package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.common.pagination.SortOrder
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.repository.SortField
import com.vaultstadio.domain.storage.repository.StorageItemQuery
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.core.SortOrder as ExposedSortOrder

internal suspend fun queryStorageItems(query: StorageItemQuery): Either<StorageException, PagedResult<StorageItem>> =
    storageDatabaseResult("Failed to query storage items") {
        newSuspendedTransaction(Dispatchers.IO) {
            val whereClause = selectionPredicate(query)

            val total = StorageItemsTable.selectAll()
                .where { whereClause }
                .count()

            val orderColumn = when (query.sortField) {
                SortField.NAME -> StorageItemsTable.name
                SortField.SIZE -> StorageItemsTable.size
                SortField.CREATED_AT -> StorageItemsTable.createdAt
                SortField.UPDATED_AT -> StorageItemsTable.updatedAt
                SortField.TYPE -> StorageItemsTable.type
            }

            val exposedSortOrder = if (query.sortOrder == SortOrder.ASC) {
                ExposedSortOrder.ASC
            } else {
                ExposedSortOrder.DESC
            }

            val items = when (query.sortField) {
                SortField.TYPE -> StorageItemsTable.selectAll()
                    .where { whereClause }
                    .orderBy(
                        StorageItemsTable.type to exposedSortOrder,
                        StorageItemsTable.name to ExposedSortOrder.ASC,
                        StorageItemsTable.id to ExposedSortOrder.ASC,
                    )
                    .limit(query.limit)
                    .offset(query.offset.toLong())
                    .map { it.toStorageItemModel() }
                else -> StorageItemsTable.selectAll()
                    .where { whereClause }
                    .orderBy(orderColumn to exposedSortOrder, StorageItemsTable.id to ExposedSortOrder.ASC)
                    .limit(query.limit)
                    .offset(query.offset.toLong())
                    .map { it.toStorageItemModel() }
            }

            PagedResult(items, total, query.offset, query.limit)
        }
    }

private fun selectionPredicate(query: StorageItemQuery): Op<Boolean> {
    var whereClause: Op<Boolean> = Op.TRUE

    query.ownerId?.let {
        whereClause = whereClause and (StorageItemsTable.ownerId eq it)
    }
    if (query.filterByParent) {
        when (query.parentId) {
            null -> whereClause = whereClause and StorageItemsTable.parentId.isNull()
            else -> whereClause = whereClause and (StorageItemsTable.parentId eq query.parentId)
        }
    }
    query.type?.let {
        whereClause = whereClause and (StorageItemsTable.type eq it.name)
    }
    query.isTrashed?.let {
        whereClause = whereClause and (StorageItemsTable.isTrashed eq it)
    }
    query.isStarred?.let {
        whereClause = whereClause and (StorageItemsTable.isStarred eq it)
    }

    return whereClause
}
