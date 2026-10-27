package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.auth.model.User
import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.domain.auth.model.UserStatus
import com.vaultstadio.domain.auth.repository.UserQuery
import com.vaultstadio.domain.auth.repository.UserRepository
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.infrastructure.persistence.entities.UsersTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

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
