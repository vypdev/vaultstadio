package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.auth.model.ApiKey
import com.vaultstadio.domain.auth.repository.ApiKeyRepository
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.infrastructure.persistence.entities.ApiKeysTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

class ExposedApiKeyRepository : ApiKeyRepository {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(apiKey: ApiKey): Either<StorageException, ApiKey> = try {
        dbQuery {
            ApiKeysTable.insert {
                it[id] = apiKey.id
                it[userId] = apiKey.userId
                it[name] = apiKey.name
                it[keyHash] = apiKey.keyHash
                it[permissions] = apiKey.permissions.joinToString(",")
                it[expiresAt] = apiKey.expiresAt
                it[lastUsedAt] = apiKey.lastUsedAt
                it[createdAt] = apiKey.createdAt
                it[isActive] = apiKey.isActive
            }
        }
        apiKey.right()
    } catch (e: Exception) {
        DatabaseException("Failed to create API key", e).left()
    }

    override suspend fun findById(id: String): Either<StorageException, ApiKey?> = try {
        dbQuery {
            ApiKeysTable.selectAll()
                .where { ApiKeysTable.id eq id }
                .map { it.toApiKey() }
                .singleOrNull()
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find API key", e).left()
    }

    override suspend fun findByKeyHash(keyHash: String): Either<StorageException, ApiKey?> = try {
        dbQuery {
            ApiKeysTable.selectAll()
                .where { ApiKeysTable.keyHash eq keyHash }
                .map { it.toApiKey() }
                .singleOrNull()
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find API key", e).left()
    }

    override suspend fun findByUserId(userId: String): Either<StorageException, List<ApiKey>> = try {
        dbQuery {
            ApiKeysTable.selectAll()
                .where { ApiKeysTable.userId eq userId }
                .map { it.toApiKey() }
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find API keys", e).left()
    }

    override suspend fun update(apiKey: ApiKey): Either<StorageException, ApiKey> = try {
        dbQuery {
            ApiKeysTable.update({ ApiKeysTable.id eq apiKey.id }) {
                it[name] = apiKey.name
                it[permissions] = apiKey.permissions.joinToString(",")
                it[expiresAt] = apiKey.expiresAt
                it[lastUsedAt] = apiKey.lastUsedAt
                it[isActive] = apiKey.isActive
            }
        }
        apiKey.right()
    } catch (e: Exception) {
        DatabaseException("Failed to update API key", e).left()
    }

    override suspend fun delete(id: String): Either<StorageException, Unit> = try {
        dbQuery {
            ApiKeysTable.deleteWhere { ApiKeysTable.id eq id }
        }
        Unit.right()
    } catch (e: Exception) {
        DatabaseException("Failed to delete API key", e).left()
    }

    override suspend fun deleteByUserId(userId: String): Either<StorageException, Unit> = try {
        dbQuery {
            ApiKeysTable.deleteWhere { ApiKeysTable.userId eq userId }
        }
        Unit.right()
    } catch (e: Exception) {
        DatabaseException("Failed to delete API keys", e).left()
    }

    private fun ResultRow.toApiKey() = ApiKey(
        id = this[ApiKeysTable.id],
        userId = this[ApiKeysTable.userId],
        name = this[ApiKeysTable.name],
        keyHash = this[ApiKeysTable.keyHash],
        permissions = this[ApiKeysTable.permissions].split(",").filter { it.isNotEmpty() },
        expiresAt = this[ApiKeysTable.expiresAt],
        lastUsedAt = this[ApiKeysTable.lastUsedAt],
        createdAt = this[ApiKeysTable.createdAt],
        isActive = this[ApiKeysTable.isActive],
    )
}
