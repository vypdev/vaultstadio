/**
 * VaultStadio User Repository Implementation
 */

package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.auth.model.ApiKey
import com.vaultstadio.domain.auth.model.User
import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.domain.auth.model.UserSession
import com.vaultstadio.domain.auth.model.UserStatus
import com.vaultstadio.domain.auth.repository.ApiKeyRepository
import com.vaultstadio.domain.auth.repository.SessionRepository
import com.vaultstadio.domain.auth.repository.UserQuery
import com.vaultstadio.domain.auth.repository.UserRepository
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.infrastructure.persistence.entities.ApiKeysTable
import com.vaultstadio.infrastructure.persistence.entities.UserSessionsTable
import com.vaultstadio.infrastructure.persistence.entities.UsersTable
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

private val logger = KotlinLogging.logger {}

class ExposedUserRepository : UserRepository {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(user: User): Either<StorageException, User> = try {
        dbQuery {
            UsersTable.insert {
                it[id] = user.id
                it[email] = user.email
                it[username] = user.username
                it[passwordHash] = user.passwordHash
                it[role] = user.role.name
                it[status] = user.status.name
                it[quotaBytes] = user.quotaBytes
                it[avatarUrl] = user.avatarUrl
                it[preferences] = user.preferences
                it[lastLoginAt] = user.lastLoginAt
                it[createdAt] = user.createdAt
                it[updatedAt] = user.updatedAt
            }
        }
        user.right()
    } catch (e: Exception) {
        DatabaseException("Failed to create user", e).left()
    }

    override suspend fun findById(id: String): Either<StorageException, User?> = try {
        dbQuery {
            UsersTable.selectAll()
                .where { UsersTable.id eq id }
                .map { it.toUser() }
                .singleOrNull()
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find user", e).left()
    }

    override suspend fun findByEmail(email: String): Either<StorageException, User?> = try {
        dbQuery {
            UsersTable.selectAll()
                .where { UsersTable.email eq email }
                .map { it.toUser() }
                .singleOrNull()
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find user", e).left()
    }

    override suspend fun findByUsername(username: String): Either<StorageException, User?> = try {
        dbQuery {
            UsersTable.selectAll()
                .where { UsersTable.username eq username }
                .map { it.toUser() }
                .singleOrNull()
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find user", e).left()
    }

    override suspend fun update(user: User): Either<StorageException, User> = try {
        dbQuery {
            UsersTable.update({ UsersTable.id eq user.id }) {
                it[email] = user.email
                it[username] = user.username
                it[passwordHash] = user.passwordHash
                it[role] = user.role.name
                it[status] = user.status.name
                it[quotaBytes] = user.quotaBytes
                it[avatarUrl] = user.avatarUrl
                it[preferences] = user.preferences
                it[lastLoginAt] = user.lastLoginAt
                it[updatedAt] = user.updatedAt
            }
        }
        user.right()
    } catch (e: Exception) {
        DatabaseException("Failed to update user", e).left()
    }

    override suspend fun delete(id: String): Either<StorageException, Unit> = try {
        dbQuery {
            UsersTable.deleteWhere { UsersTable.id eq id }
        }
        Unit.right()
    } catch (e: Exception) {
        DatabaseException("Failed to delete user", e).left()
    }

    override suspend fun existsByEmail(email: String): Either<StorageException, Boolean> = try {
        dbQuery {
            UsersTable.selectAll()
                .where { UsersTable.email eq email }
                .count() > 0
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to check email", e).left()
    }

    override suspend fun existsByUsername(username: String): Either<StorageException, Boolean> = try {
        dbQuery {
            UsersTable.selectAll()
                .where { UsersTable.username eq username }
                .count() > 0
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to check username", e).left()
    }

    override suspend fun countAll(): Either<StorageException, Long> = try {
        dbQuery {
            UsersTable.selectAll().count()
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to count users", e).left()
    }

    override suspend fun updateLastLogin(userId: String): Either<StorageException, Unit> = try {
        dbQuery {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[lastLoginAt] = Clock.System.now()
            }
        }
        Unit.right()
    } catch (e: Exception) {
        DatabaseException("Failed to update last login", e).left()
    }

    override suspend fun query(query: UserQuery): Either<StorageException, PagedResult<User>> = try {
        dbQuery {
            val total = UsersTable.selectAll().count()
            val items = UsersTable.selectAll()
                .limit(query.limit)
                .offset(query.offset.toLong())
                .map { it.toUser() }
            PagedResult(items, total, query.offset, query.limit)
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to query users", e).left()
    }

    private fun ResultRow.toUser() = User(
        id = this[UsersTable.id],
        email = this[UsersTable.email],
        username = this[UsersTable.username],
        passwordHash = this[UsersTable.passwordHash],
        role = UserRole.valueOf(this[UsersTable.role]),
        status = UserStatus.valueOf(this[UsersTable.status]),
        quotaBytes = this[UsersTable.quotaBytes],
        avatarUrl = this[UsersTable.avatarUrl],
        preferences = this[UsersTable.preferences],
        lastLoginAt = this[UsersTable.lastLoginAt],
        createdAt = this[UsersTable.createdAt],
        updatedAt = this[UsersTable.updatedAt],
    )
}

class ExposedSessionRepository : SessionRepository {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(session: UserSession): Either<StorageException, UserSession> = try {
        dbQuery {
            UserSessionsTable.insert {
                it[id] = session.id
                it[userId] = session.userId
                it[tokenHash] = session.tokenHash
                it[refreshTokenHash] = session.refreshTokenHash
                it[ipAddress] = session.ipAddress
                it[userAgent] = session.userAgent
                it[expiresAt] = session.expiresAt
                it[createdAt] = session.createdAt
                it[lastActivityAt] = session.lastActivityAt
            }
        }
        session.right()
    } catch (e: Exception) {
        DatabaseException("Failed to create session", e).left()
    }

    override suspend fun findById(id: String): Either<StorageException, UserSession?> = try {
        dbQuery {
            UserSessionsTable.selectAll()
                .where { UserSessionsTable.id eq id }
                .map { it.toSession() }
                .singleOrNull()
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find session", e).left()
    }

    override suspend fun findByTokenHash(tokenHash: String): Either<StorageException, UserSession?> = try {
        dbQuery {
            UserSessionsTable.selectAll()
                .where { UserSessionsTable.tokenHash eq tokenHash }
                .map { it.toSession() }
                .singleOrNull()
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find session", e).left()
    }

    override suspend fun findByRefreshTokenHash(
        refreshTokenHash: String,
    ): Either<StorageException, UserSession?> = try {
        dbQuery {
            UserSessionsTable.selectAll()
                .where { UserSessionsTable.refreshTokenHash eq refreshTokenHash }
                .map { it.toSession() }
                .singleOrNull()
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find session by refresh token", e).left()
    }

    override suspend fun findByUserId(userId: String): Either<StorageException, List<UserSession>> = try {
        dbQuery {
            UserSessionsTable.selectAll()
                .where { UserSessionsTable.userId eq userId }
                .map { it.toSession() }
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find sessions", e).left()
    }

    override suspend fun delete(id: String): Either<StorageException, Unit> = try {
        dbQuery {
            UserSessionsTable.deleteWhere { UserSessionsTable.id eq id }
        }
        Unit.right()
    } catch (e: Exception) {
        DatabaseException("Failed to delete session", e).left()
    }

    override suspend fun deleteByUserId(userId: String): Either<StorageException, Unit> = try {
        dbQuery {
            UserSessionsTable.deleteWhere { UserSessionsTable.userId eq userId }
        }
        Unit.right()
    } catch (e: Exception) {
        DatabaseException("Failed to delete sessions", e).left()
    }

    override suspend fun update(session: UserSession): Either<StorageException, UserSession> = try {
        dbQuery {
            UserSessionsTable.update({ UserSessionsTable.id eq session.id }) {
                it[tokenHash] = session.tokenHash
                it[refreshTokenHash] = session.refreshTokenHash
                it[expiresAt] = session.expiresAt
                it[lastActivityAt] = session.lastActivityAt
            }
        }
        session.right()
    } catch (e: Exception) {
        DatabaseException("Failed to update session", e).left()
    }

    override suspend fun deleteExpired(): Either<StorageException, Int> = try {
        dbQuery {
            UserSessionsTable.deleteWhere { UserSessionsTable.expiresAt lessEq Clock.System.now() }
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to delete expired sessions", e).left()
    }

    override suspend fun updateLastActivity(id: String): Either<StorageException, Unit> = try {
        dbQuery {
            UserSessionsTable.update({ UserSessionsTable.id eq id }) {
                it[lastActivityAt] = Clock.System.now()
            }
        }
        Unit.right()
    } catch (e: Exception) {
        DatabaseException("Failed to update last activity", e).left()
    }

    private fun ResultRow.toSession() = UserSession(
        id = this[UserSessionsTable.id],
        userId = this[UserSessionsTable.userId],
        tokenHash = this[UserSessionsTable.tokenHash],
        refreshTokenHash = this[UserSessionsTable.refreshTokenHash],
        ipAddress = this[UserSessionsTable.ipAddress],
        userAgent = this[UserSessionsTable.userAgent],
        expiresAt = this[UserSessionsTable.expiresAt],
        createdAt = this[UserSessionsTable.createdAt],
        lastActivityAt = this[UserSessionsTable.lastActivityAt],
    )
}

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
