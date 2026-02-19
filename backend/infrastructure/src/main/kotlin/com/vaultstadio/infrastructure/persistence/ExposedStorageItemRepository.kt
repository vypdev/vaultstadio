/**
 * VaultStadio Storage Item Repository Implementation
 */

package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.ItemType
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.model.StorageQuota
import com.vaultstadio.core.domain.model.Visibility
import com.vaultstadio.core.domain.repository.PagedResult
import com.vaultstadio.core.domain.repository.SortField
import com.vaultstadio.core.domain.repository.SortOrder
import com.vaultstadio.core.domain.repository.StorageItemQuery
import com.vaultstadio.core.domain.repository.StorageItemRepository
import com.vaultstadio.core.exception.DatabaseException
import com.vaultstadio.core.exception.StorageException
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SortOrder as ExposedSortOrder

private val logger = KotlinLogging.logger {}

class ExposedStorageItemRepository : StorageItemRepository {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(item: StorageItem): Either<StorageException, StorageItem> = try {
        dbQuery {
            StorageItemsTable.insert {
                it[id] = item.id
                it[name] = item.name
                it[path] = item.path
                it[type] = item.type.name
                it[parentId] = item.parentId
                it[ownerId] = item.ownerId
                it[size] = item.size
                it[mimeType] = item.mimeType
                it[checksum] = item.checksum
                it[storageKey] = item.storageKey
                it[visibility] = item.visibility.name
                it[isTrashed] = item.isTrashed
                it[isStarred] = item.isStarred
                it[createdAt] = item.createdAt
                it[updatedAt] = item.updatedAt
                it[trashedAt] = item.trashedAt
                it[version] = item.version
            }
        }
        item.right()
    } catch (e: Exception) {
        DatabaseException("Failed to create storage item", e).left()
    }

    override suspend fun findById(id: String): Either<StorageException, StorageItem?> = try {
        dbQuery {
            StorageItemsTable.selectAll()
                .where { StorageItemsTable.id eq id }
                .map { it.toStorageItem() }
                .singleOrNull()
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find storage item", e).left()
    }

    override suspend fun findByPath(path: String, ownerId: String): Either<StorageException, StorageItem?> = try {
        dbQuery {
            StorageItemsTable.selectAll()
                .where { (StorageItemsTable.path eq path) and (StorageItemsTable.ownerId eq ownerId) }
                .map { it.toStorageItem() }
                .singleOrNull()
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find storage item by path", e).left()
    }

    override suspend fun update(item: StorageItem): Either<StorageException, StorageItem> = try {
        dbQuery {
            StorageItemsTable.update({ StorageItemsTable.id eq item.id }) {
                it[name] = item.name
                it[path] = item.path
                it[type] = item.type.name
                it[parentId] = item.parentId
                it[size] = item.size
                it[mimeType] = item.mimeType
                it[checksum] = item.checksum
                it[storageKey] = item.storageKey
                it[visibility] = item.visibility.name
                it[isTrashed] = item.isTrashed
                it[isStarred] = item.isStarred
                it[updatedAt] = item.updatedAt
                it[trashedAt] = item.trashedAt
                it[version] = item.version
            }
        }
        item.right()
    } catch (e: Exception) {
        DatabaseException("Failed to update storage item", e).left()
    }

    override suspend fun delete(id: String): Either<StorageException, Unit> = try {
        dbQuery {
            StorageItemsTable.deleteWhere { StorageItemsTable.id eq id }
        }
        Unit.right()
    } catch (e: Exception) {
        DatabaseException("Failed to delete storage item", e).left()
    }

    override suspend fun query(query: StorageItemQuery): Either<StorageException, PagedResult<StorageItem>> = try {
        dbQuery {
            var whereClause: Op<Boolean> = Op.TRUE

            query.ownerId?.let {
                whereClause = whereClause and (StorageItemsTable.ownerId eq it)
            }
            if (query.filterByParent) {
                when (query.parentId) {
                    null -> whereClause = whereClause and (StorageItemsTable.parentId.isNull())
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
                    )
                    .limit(query.limit)
                    .offset(query.offset.toLong())
                    .map { it.toStorageItem() }
                else -> StorageItemsTable.selectAll()
                    .where { whereClause }
                    .orderBy(orderColumn, exposedSortOrder)
                    .limit(query.limit)
                    .offset(query.offset.toLong())
                    .map { it.toStorageItem() }
            }

            PagedResult(items, total, query.offset, query.limit)
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to query storage items", e).left()
    }

    override suspend fun listChildren(
        parentId: String?,
        ownerId: String,
    ): Either<StorageException, List<StorageItem>> = try {
        dbQuery {
            val items = if (parentId != null) {
                StorageItemsTable.selectAll()
                    .where {
                        (StorageItemsTable.ownerId eq ownerId) and (StorageItemsTable.parentId eq parentId)
                    }
                    .map { it.toStorageItem() }
            } else {
                StorageItemsTable.selectAll()
                    .where {
                        (StorageItemsTable.ownerId eq ownerId) and (StorageItemsTable.parentId.isNull())
                    }
                    .map { it.toStorageItem() }
            }
            items
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to list children", e).left()
    }

    override suspend fun getAncestors(itemId: String): Either<StorageException, List<StorageItem>> = try {
        dbQuery {
            val ancestors = mutableListOf<StorageItem>()
            var currentItem = StorageItemsTable.selectAll()
                .where { StorageItemsTable.id eq itemId }
                .map { it.toStorageItem() }
                .singleOrNull()

            while (currentItem?.parentId != null) {
                currentItem = StorageItemsTable.selectAll()
                    .where { StorageItemsTable.id eq currentItem!!.parentId!! }
                    .map { it.toStorageItem() }
                    .singleOrNull()

                currentItem?.let { ancestors.add(0, it) }
            }

            ancestors
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to get ancestors", e).left()
    }

    override suspend fun move(
        itemId: String,
        newParentId: String?,
        newName: String,
    ): Either<StorageException, StorageItem> = try {
        dbQuery {
            val item = StorageItemsTable.selectAll()
                .where { StorageItemsTable.id eq itemId }
                .map { it.toStorageItem() }
                .single()

            val newPath = if (newParentId != null) {
                val parent = StorageItemsTable.selectAll()
                    .where { StorageItemsTable.id eq newParentId }
                    .map { it.toStorageItem() }
                    .single()
                "${parent.path}/$newName"
            } else {
                "/$newName"
            }

            StorageItemsTable.update({ StorageItemsTable.id eq itemId }) {
                it[parentId] = newParentId
                it[name] = newName
                it[path] = newPath
            }

            item.copy(parentId = newParentId, name = newName, path = newPath)
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to move item", e).left()
    }

    override suspend fun existsByPath(path: String, ownerId: String): Either<StorageException, Boolean> = try {
        dbQuery {
            StorageItemsTable.selectAll()
                .where { (StorageItemsTable.path eq path) and (StorageItemsTable.ownerId eq ownerId) }
                .count() > 0
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to check path", e).left()
    }

    override suspend fun getQuota(userId: String): Either<StorageException, StorageQuota> = try {
        dbQuery {
            val usedBytes = StorageItemsTable.selectAll()
                .where { (StorageItemsTable.ownerId eq userId) and (StorageItemsTable.type eq ItemType.FILE.name) }
                .sumOf { it[StorageItemsTable.size] }

            val fileCount = StorageItemsTable.selectAll()
                .where { (StorageItemsTable.ownerId eq userId) and (StorageItemsTable.type eq ItemType.FILE.name) }
                .count()

            val folderCount = StorageItemsTable.selectAll()
                .where { (StorageItemsTable.ownerId eq userId) and (StorageItemsTable.type eq ItemType.FOLDER.name) }
                .count()

            StorageQuota(
                userId = userId,
                usedBytes = usedBytes,
                quotaBytes = null,
                fileCount = fileCount,
                folderCount = folderCount,
            )
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to get quota", e).left()
    }

    override fun stream(query: StorageItemQuery): Flow<StorageItem> = emptyFlow()

    override suspend fun count(query: StorageItemQuery): Either<StorageException, Long> = try {
        dbQuery {
            StorageItemsTable.selectAll().count()
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to count items", e).left()
    }

    override suspend fun calculateTotalSize(ownerId: String): Either<StorageException, Long> = try {
        dbQuery {
            StorageItemsTable.selectAll()
                .where { (StorageItemsTable.ownerId eq ownerId) and (StorageItemsTable.type eq ItemType.FILE.name) }
                .sumOf { it[StorageItemsTable.size] }
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to calculate total size", e).left()
    }

    private fun ResultRow.toStorageItem() = StorageItem(
        id = this[StorageItemsTable.id],
        name = this[StorageItemsTable.name],
        path = this[StorageItemsTable.path],
        type = ItemType.valueOf(this[StorageItemsTable.type]),
        parentId = this[StorageItemsTable.parentId],
        ownerId = this[StorageItemsTable.ownerId],
        size = this[StorageItemsTable.size],
        mimeType = this[StorageItemsTable.mimeType],
        checksum = this[StorageItemsTable.checksum],
        storageKey = this[StorageItemsTable.storageKey],
        visibility = Visibility.valueOf(this[StorageItemsTable.visibility]),
        isTrashed = this[StorageItemsTable.isTrashed],
        isStarred = this[StorageItemsTable.isStarred],
        createdAt = this[StorageItemsTable.createdAt],
        updatedAt = this[StorageItemsTable.updatedAt],
        trashedAt = this[StorageItemsTable.trashedAt],
        version = this[StorageItemsTable.version],
    )
}
