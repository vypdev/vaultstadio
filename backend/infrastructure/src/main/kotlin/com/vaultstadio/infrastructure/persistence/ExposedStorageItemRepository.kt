/**
 * VaultStadio Storage Item Repository Implementation
 */

package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.model.StorageQuota
import com.vaultstadio.domain.storage.repository.StorageItemQuery
import com.vaultstadio.domain.storage.repository.StorageItemRepository
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

class ExposedStorageItemRepository : StorageItemRepository {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(item: StorageItem): Either<StorageException, StorageItem> = storageDatabaseResult(
        "Failed to create storage item",
    ) {
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
        item
    }

    override suspend fun findById(id: String): Either<StorageException, StorageItem?> = storageDatabaseResult(
        "Failed to find storage item",
    ) {
        dbQuery {
            StorageItemsTable.selectAll()
                .where { StorageItemsTable.id eq id }
                .map { it.toStorageItemModel() }
                .singleOrNull()
        }
    }

    override suspend fun findByPath(
        path: String,
        ownerId: String,
    ): Either<StorageException, StorageItem?> = storageDatabaseResult(
        "Failed to find storage item by path",
    ) {
        dbQuery {
            StorageItemsTable.selectAll()
                .where { StorageItemsTable.path eq path and (StorageItemsTable.ownerId eq ownerId) }
                .map { it.toStorageItemModel() }
                .singleOrNull()
        }
    }

    override suspend fun update(item: StorageItem): Either<StorageException, StorageItem> = storageDatabaseResult(
        "Failed to update storage item",
    ) {
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
        item
    }

    override suspend fun delete(id: String): Either<StorageException, Unit> = deleteStorageLeaf(id).map { Unit }

    override suspend fun deleteLeaf(
        id: String,
        actorId: String,
        deleteContent: suspend (StorageItem) -> Either<StorageException, Unit>,
    ): Either<StorageException, StorageItem> = deleteStorageLeaf(id, actorId, deleteContent).fold(
        { it.left() },
        { item -> item?.right() ?: ItemNotFoundException(itemId = id).left() },
    )

    override suspend fun query(query: StorageItemQuery): Either<StorageException, PagedResult<StorageItem>> =
        queryStorageItems(query)

    override suspend fun listChildren(
        parentId: String?,
        ownerId: String,
    ): Either<StorageException, List<StorageItem>> = storageDatabaseResult("Failed to list children") {
        dbQuery {
            val items = if (parentId != null) {
                StorageItemsTable.selectAll()
                    .where {
                        StorageItemsTable.ownerId eq ownerId and (StorageItemsTable.parentId eq parentId)
                    }
                    .map { it.toStorageItemModel() }
            } else {
                StorageItemsTable.selectAll()
                    .where {
                        StorageItemsTable.ownerId eq ownerId and StorageItemsTable.parentId.isNull()
                    }
                    .map { it.toStorageItemModel() }
            }
            items
        }
    }

    override suspend fun getAncestors(
        itemId: String,
    ): Either<StorageException, List<StorageItem>> = storageDatabaseResult(
        "Failed to get ancestors",
    ) {
        dbQuery {
            val ancestors = mutableListOf<StorageItem>()
            var currentItem = StorageItemsTable.selectAll()
                .where { StorageItemsTable.id eq itemId }
                .map { it.toStorageItemModel() }
                .singleOrNull()

            while (currentItem?.parentId != null) {
                currentItem = StorageItemsTable.selectAll()
                    .where { StorageItemsTable.id eq currentItem!!.parentId!! }
                    .map { it.toStorageItemModel() }
                    .singleOrNull()

                currentItem?.let { ancestors.add(0, it) }
            }

            ancestors
        }
    }

    override suspend fun move(
        itemId: String,
        newParentId: String?,
        newName: String,
    ): Either<StorageException, StorageItem> = storageDatabaseResult("Failed to move item") {
        dbQuery {
            val item = StorageItemsTable.selectAll()
                .where { StorageItemsTable.id eq itemId }
                .map { it.toStorageItemModel() }
                .single()

            val newPath = if (newParentId != null) {
                val parent = StorageItemsTable.selectAll()
                    .where { StorageItemsTable.id eq newParentId }
                    .map { it.toStorageItemModel() }
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
        }
    }

    override suspend fun existsByPath(
        path: String,
        ownerId: String,
    ): Either<StorageException, Boolean> = storageDatabaseResult(
        "Failed to check path",
    ) {
        dbQuery {
            StorageItemsTable.selectAll()
                .where { StorageItemsTable.path eq path and (StorageItemsTable.ownerId eq ownerId) }
                .count() > 0
        }
    }

    override suspend fun getQuota(userId: String): Either<StorageException, StorageQuota> = storageDatabaseResult(
        "Failed to get quota",
    ) {
        dbQuery {
            val usedBytes = StorageItemsTable.selectAll()
                .where { StorageItemsTable.ownerId eq userId and (StorageItemsTable.type eq ItemType.FILE.name) }
                .sumOf { it[StorageItemsTable.size] }

            val fileCount = StorageItemsTable.selectAll()
                .where { StorageItemsTable.ownerId eq userId and (StorageItemsTable.type eq ItemType.FILE.name) }
                .count()

            val folderCount = StorageItemsTable.selectAll()
                .where { StorageItemsTable.ownerId eq userId and (StorageItemsTable.type eq ItemType.FOLDER.name) }
                .count()

            StorageQuota(
                userId = userId,
                usedBytes = usedBytes,
                quotaBytes = null,
                fileCount = fileCount,
                folderCount = folderCount,
            )
        }
    }

    override fun stream(query: StorageItemQuery): Flow<StorageItem> = emptyFlow()

    override suspend fun count(query: StorageItemQuery): Either<StorageException, Long> = storageDatabaseResult(
        "Failed to count items",
    ) {
        dbQuery {
            StorageItemsTable.selectAll().count()
        }
    }

    override suspend fun calculateTotalSize(ownerId: String): Either<StorageException, Long> = storageDatabaseResult(
        "Failed to calculate total size",
    ) {
        dbQuery {
            StorageItemsTable.selectAll()
                .where { StorageItemsTable.ownerId eq ownerId and (StorageItemsTable.type eq ItemType.FILE.name) }
                .sumOf { it[StorageItemsTable.size] }
        }
    }
}
