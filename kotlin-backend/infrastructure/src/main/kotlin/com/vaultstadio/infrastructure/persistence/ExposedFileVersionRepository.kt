/**
 * VaultStadio Exposed File Version Repository Implementation
 */

package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.model.FileVersionHistory
import com.vaultstadio.core.domain.model.ItemType
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.model.VersionRetentionPolicy
import com.vaultstadio.core.domain.model.Visibility
import com.vaultstadio.core.domain.repository.FileVersionRepository
import com.vaultstadio.core.exception.DatabaseException
import com.vaultstadio.core.exception.StorageException
import com.vaultstadio.infrastructure.persistence.entities.FileVersionsTable
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import kotlin.time.Duration.Companion.days

/**
 * Exposed implementation of FileVersionRepository.
 */
class ExposedFileVersionRepository : FileVersionRepository {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(version: FileVersion): Either<StorageException, FileVersion> {
        return try {
            dbQuery {
                FileVersionsTable.insert {
                    it[id] = version.id
                    it[itemId] = version.itemId
                    it[versionNumber] = version.versionNumber
                    it[size] = version.size
                    it[checksum] = version.checksum
                    it[storageKey] = version.storageKey
                    it[createdBy] = version.createdBy
                    it[createdAt] = version.createdAt
                    it[comment] = version.comment
                    it[isLatest] = version.isLatest
                    it[restoredFrom] = version.restoredFrom
                }
            }
            version.right()
        } catch (e: Exception) {
            DatabaseException("Failed to create version: ${e.message}", e).left()
        }
    }

    override suspend fun findById(id: String): Either<StorageException, FileVersion?> {
        return try {
            dbQuery {
                FileVersionsTable.selectAll()
                    .where { FileVersionsTable.id eq id }
                    .map { it.toFileVersion() }
                    .singleOrNull()
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to find version: ${e.message}", e).left()
        }
    }

    override suspend fun findByItemAndVersion(
        itemId: String,
        versionNumber: Int,
    ): Either<StorageException, FileVersion?> {
        return try {
            dbQuery {
                FileVersionsTable.selectAll()
                    .where {
                        (FileVersionsTable.itemId eq itemId) and
                            (FileVersionsTable.versionNumber eq versionNumber)
                    }
                    .map { it.toFileVersion() }
                    .singleOrNull()
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to find version: ${e.message}", e).left()
        }
    }

    override suspend fun findLatest(itemId: String): Either<StorageException, FileVersion?> {
        return try {
            dbQuery {
                FileVersionsTable.selectAll()
                    .where {
                        (FileVersionsTable.itemId eq itemId) and
                            (FileVersionsTable.isLatest eq true)
                    }
                    .map { it.toFileVersion() }
                    .singleOrNull()
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to find latest version: ${e.message}", e).left()
        }
    }

    override suspend fun listVersions(
        itemId: String,
        limit: Int,
        offset: Int,
    ): Either<StorageException, List<FileVersion>> {
        return try {
            dbQuery {
                FileVersionsTable.selectAll()
                    .where { FileVersionsTable.itemId eq itemId }
                    .orderBy(FileVersionsTable.versionNumber, SortOrder.DESC)
                    .limit(limit, offset.toLong())
                    .map { it.toFileVersion() }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to list versions: ${e.message}", e).left()
        }
    }

    override suspend fun getHistory(itemId: String): Either<StorageException, FileVersionHistory> {
        return try {
            dbQuery {
                val versions = FileVersionsTable.selectAll()
                    .where { FileVersionsTable.itemId eq itemId }
                    .orderBy(FileVersionsTable.versionNumber, SortOrder.DESC)
                    .map { it.toFileVersion() }

                val item = StorageItemsTable.selectAll()
                    .where { StorageItemsTable.id eq itemId }
                    .map { it.toStorageItem() }
                    .singleOrNull()

                FileVersionHistory(
                    item = item ?: StorageItem(
                        id = itemId,
                        name = "Unknown",
                        path = "/",
                        type = ItemType.FILE,
                        ownerId = "",
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    ),
                    versions = versions,
                    totalVersions = versions.size,
                    totalSize = versions.sumOf { it.size },
                )
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get history: ${e.message}", e).left()
        }
    }

    override suspend fun getNextVersionNumber(itemId: String): Either<StorageException, Int> {
        return try {
            dbQuery {
                val maxVersionExpr = FileVersionsTable.versionNumber.max()
                val maxVersion = FileVersionsTable
                    .select(maxVersionExpr)
                    .where { FileVersionsTable.itemId eq itemId }
                    .singleOrNull()
                    ?.getOrNull(maxVersionExpr)

                (maxVersion ?: 0) + 1
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get next version number: ${e.message}", e).left()
        }
    }

    override suspend fun update(version: FileVersion): Either<StorageException, FileVersion> {
        return try {
            dbQuery {
                FileVersionsTable.update({ FileVersionsTable.id eq version.id }) {
                    it[isLatest] = version.isLatest
                    it[comment] = version.comment
                }
            }
            version.right()
        } catch (e: Exception) {
            DatabaseException("Failed to update version: ${e.message}", e).left()
        }
    }

    override suspend fun delete(id: String): Either<StorageException, Unit> {
        return try {
            dbQuery {
                FileVersionsTable.deleteWhere { FileVersionsTable.id eq id }
            }
            Unit.right()
        } catch (e: Exception) {
            DatabaseException("Failed to delete version: ${e.message}", e).left()
        }
    }

    override suspend fun deleteAllForItem(itemId: String): Either<StorageException, Int> {
        return try {
            dbQuery {
                FileVersionsTable.deleteWhere { FileVersionsTable.itemId eq itemId }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to delete versions: ${e.message}", e).left()
        }
    }

    override suspend fun applyRetentionPolicy(
        itemId: String,
        policy: VersionRetentionPolicy,
    ): Either<StorageException, List<String>> {
        return try {
            dbQuery {
                val versions = FileVersionsTable.selectAll()
                    .where { FileVersionsTable.itemId eq itemId }
                    .orderBy(FileVersionsTable.versionNumber, SortOrder.DESC)
                    .map { it.toFileVersion() }

                val toDelete = mutableListOf<String>()
                val now = Clock.System.now()

                val maxVersions = policy.maxVersions
                val maxAgeDays = policy.maxAgeDays

                versions.forEachIndexed { index, version ->
                    // Keep minimum versions
                    if (index < policy.minVersionsToKeep) return@forEachIndexed

                    // Check max versions
                    if (maxVersions != null && index >= maxVersions) {
                        toDelete.add(version.id)
                        return@forEachIndexed
                    }

                    // Check max age
                    if (maxAgeDays != null) {
                        val ageThreshold = now.minus(maxAgeDays.days)
                        if (version.createdAt < ageThreshold) {
                            toDelete.add(version.id)
                        }
                    }
                }

                // Delete marked versions
                if (toDelete.isNotEmpty()) {
                    FileVersionsTable.deleteWhere {
                        FileVersionsTable.id inList toDelete
                    }
                }

                toDelete
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to apply retention policy: ${e.message}", e).left()
        }
    }

    override suspend fun countVersions(itemId: String): Either<StorageException, Int> {
        return try {
            dbQuery {
                FileVersionsTable.selectAll()
                    .where { FileVersionsTable.itemId eq itemId }
                    .count()
                    .toInt()
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to count versions: ${e.message}", e).left()
        }
    }

    override suspend fun getTotalVersionSize(itemId: String): Either<StorageException, Long> {
        return try {
            dbQuery {
                FileVersionsTable
                    .select(FileVersionsTable.size.sum())
                    .where { FileVersionsTable.itemId eq itemId }
                    .singleOrNull()
                    ?.get(FileVersionsTable.size.sum()) ?: 0L
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get total version size: ${e.message}", e).left()
        }
    }

    override fun streamVersions(itemId: String?): Flow<FileVersion> = flow {
        val versions = dbQuery {
            val query = if (itemId != null) {
                FileVersionsTable.selectAll()
                    .where { FileVersionsTable.itemId eq itemId }
            } else {
                FileVersionsTable.selectAll()
            }
            query.orderBy(FileVersionsTable.createdAt, SortOrder.DESC)
                .map { it.toFileVersion() }
        }
        versions.forEach { emit(it) }
    }

    override suspend fun findByStorageKey(storageKey: String): Either<StorageException, List<FileVersion>> {
        return try {
            dbQuery {
                FileVersionsTable.selectAll()
                    .where { FileVersionsTable.storageKey eq storageKey }
                    .map { it.toFileVersion() }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to find by storage key: ${e.message}", e).left()
        }
    }

    private fun ResultRow.toFileVersion(): FileVersion = FileVersion(
        id = this[FileVersionsTable.id],
        itemId = this[FileVersionsTable.itemId],
        versionNumber = this[FileVersionsTable.versionNumber],
        size = this[FileVersionsTable.size],
        checksum = this[FileVersionsTable.checksum],
        storageKey = this[FileVersionsTable.storageKey],
        createdBy = this[FileVersionsTable.createdBy],
        createdAt = this[FileVersionsTable.createdAt],
        comment = this[FileVersionsTable.comment],
        isLatest = this[FileVersionsTable.isLatest],
        restoredFrom = this[FileVersionsTable.restoredFrom],
    )

    private fun ResultRow.toStorageItem(): StorageItem = StorageItem(
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
