/**
 * VaultStadio Metadata Repository Implementation
 */

package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.StorageItemMetadata
import com.vaultstadio.core.domain.repository.MetadataRepository
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.infrastructure.persistence.entities.StorageItemMetadataTable
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

private val logger = KotlinLogging.logger {}

class ExposedMetadataRepository : MetadataRepository {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun save(metadata: StorageItemMetadata): Either<StorageException, StorageItemMetadata> = try {
        dbQuery {
            // Check if exists
            val existing = StorageItemMetadataTable.selectAll()
                .where {
                    (StorageItemMetadataTable.itemId eq metadata.itemId) and
                        (StorageItemMetadataTable.pluginId eq metadata.pluginId) and
                        (StorageItemMetadataTable.key eq metadata.key)
                }
                .singleOrNull()

            if (existing != null) {
                // Update
                StorageItemMetadataTable.update({
                    (StorageItemMetadataTable.itemId eq metadata.itemId) and
                        (StorageItemMetadataTable.pluginId eq metadata.pluginId) and
                        (StorageItemMetadataTable.key eq metadata.key)
                }) {
                    it[value] = metadata.value
                    it[updatedAt] = Clock.System.now()
                }
                metadata.copy(id = existing[StorageItemMetadataTable.id], updatedAt = Clock.System.now())
            } else {
                // Insert
                val id = metadata.id.ifEmpty { UUID.randomUUID().toString() }
                StorageItemMetadataTable.insert {
                    it[StorageItemMetadataTable.id] = id
                    it[itemId] = metadata.itemId
                    it[pluginId] = metadata.pluginId
                    it[key] = metadata.key
                    it[value] = metadata.value
                    it[createdAt] = metadata.createdAt
                    it[updatedAt] = metadata.updatedAt
                }
                metadata.copy(id = id)
            }
        }.right()
    } catch (e: Exception) {
        logger.error(e) { "Failed to save metadata" }
        DatabaseException("Failed to save metadata", e).left()
    }

    override suspend fun saveAll(
        metadataList: List<StorageItemMetadata>,
    ): Either<StorageException, List<StorageItemMetadata>> = try {
        val results = metadataList.map { save(it) }
        val errors = results.mapNotNull { it.leftOrNull() }
        if (errors.isNotEmpty()) {
            errors.first().left()
        } else {
            results.mapNotNull { it.getOrNull() }.right()
        }
    } catch (e: Exception) {
        DatabaseException("Failed to save metadata batch", e).left()
    }

    override suspend fun findById(id: String): Either<StorageException, StorageItemMetadata?> = try {
        dbQuery {
            StorageItemMetadataTable.selectAll()
                .where { StorageItemMetadataTable.id eq id }
                .map { it.toMetadata() }
                .singleOrNull()
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find metadata", e).left()
    }

    override suspend fun findByItemId(itemId: String): Either<StorageException, List<StorageItemMetadata>> = try {
        dbQuery {
            StorageItemMetadataTable.selectAll()
                .where { StorageItemMetadataTable.itemId eq itemId }
                .map { it.toMetadata() }
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find metadata by item", e).left()
    }

    override suspend fun findByItemIdAndPluginId(
        itemId: String,
        pluginId: String,
    ): Either<StorageException, List<StorageItemMetadata>> = try {
        dbQuery {
            StorageItemMetadataTable.selectAll()
                .where {
                    (StorageItemMetadataTable.itemId eq itemId) and
                        (StorageItemMetadataTable.pluginId eq pluginId)
                }
                .map { it.toMetadata() }
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find metadata", e).left()
    }

    override suspend fun findByItemIdAndPluginIdAndKey(
        itemId: String,
        pluginId: String,
        key: String,
    ): Either<StorageException, StorageItemMetadata?> = try {
        dbQuery {
            StorageItemMetadataTable.selectAll()
                .where {
                    (StorageItemMetadataTable.itemId eq itemId) and
                        (StorageItemMetadataTable.pluginId eq pluginId) and
                        (StorageItemMetadataTable.key eq key)
                }
                .map { it.toMetadata() }
                .singleOrNull()
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find metadata", e).left()
    }

    override suspend fun findByPluginId(
        pluginId: String,
        limit: Int,
        offset: Int,
    ): Either<StorageException, PagedResult<StorageItemMetadata>> = try {
        dbQuery {
            val total = StorageItemMetadataTable.selectAll()
                .where { StorageItemMetadataTable.pluginId eq pluginId }
                .count()

            val items = StorageItemMetadataTable.selectAll()
                .where { StorageItemMetadataTable.pluginId eq pluginId }
                .limit(limit)
                .offset(offset.toLong())
                .map { it.toMetadata() }

            PagedResult(items, total, offset, limit)
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find metadata by plugin", e).left()
    }

    override suspend fun delete(id: String): Either<StorageException, Unit> = try {
        dbQuery {
            StorageItemMetadataTable.deleteWhere { StorageItemMetadataTable.id eq id }
        }
        Unit.right()
    } catch (e: Exception) {
        DatabaseException("Failed to delete metadata", e).left()
    }

    override suspend fun deleteByItemId(itemId: String): Either<StorageException, Int> = try {
        dbQuery {
            StorageItemMetadataTable.deleteWhere { StorageItemMetadataTable.itemId eq itemId }
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to delete metadata", e).left()
    }

    override suspend fun deleteByItemIdAndPluginId(
        itemId: String,
        pluginId: String,
    ): Either<StorageException, Int> = try {
        dbQuery {
            StorageItemMetadataTable.deleteWhere {
                (StorageItemMetadataTable.itemId eq itemId) and
                    (StorageItemMetadataTable.pluginId eq pluginId)
            }
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to delete metadata", e).left()
    }

    override suspend fun deleteByPluginId(pluginId: String): Either<StorageException, Int> = try {
        dbQuery {
            StorageItemMetadataTable.deleteWhere { StorageItemMetadataTable.pluginId eq pluginId }
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to delete metadata by plugin", e).left()
    }

    override suspend fun searchByValue(
        pluginId: String,
        key: String,
        valuePattern: String,
        limit: Int,
        offset: Int,
    ): Either<StorageException, List<String>> = try {
        dbQuery {
            StorageItemMetadataTable.selectAll()
                .where {
                    (StorageItemMetadataTable.pluginId eq pluginId) and
                        (StorageItemMetadataTable.key eq key) and
                        (StorageItemMetadataTable.value like "%$valuePattern%")
                }
                .limit(limit)
                .offset(offset.toLong())
                .map { it[StorageItemMetadataTable.itemId] }
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to search metadata", e).left()
    }

    override suspend fun searchByKeyValue(
        key: String,
        valuePattern: String,
        pluginId: String?,
        limit: Int,
    ): Either<StorageException, List<StorageItemMetadata>> = try {
        dbQuery {
            val query = StorageItemMetadataTable.selectAll()
                .where {
                    val keyCondition = StorageItemMetadataTable.key eq key
                    val valueCondition = StorageItemMetadataTable.value like "%$valuePattern%"

                    if (pluginId != null) {
                        keyCondition and valueCondition and (StorageItemMetadataTable.pluginId eq pluginId)
                    } else {
                        keyCondition and valueCondition
                    }
                }
                .limit(limit)

            query.map { it.toMetadata() }
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to search metadata by key-value", e).left()
    }

    private fun ResultRow.toMetadata() = StorageItemMetadata(
        id = this[StorageItemMetadataTable.id],
        itemId = this[StorageItemMetadataTable.itemId],
        pluginId = this[StorageItemMetadataTable.pluginId],
        key = this[StorageItemMetadataTable.key],
        value = this[StorageItemMetadataTable.value],
        createdAt = this[StorageItemMetadataTable.createdAt],
        updatedAt = this[StorageItemMetadataTable.updatedAt],
    )
}
