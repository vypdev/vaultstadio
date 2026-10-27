package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.auth.model.UserSession
import com.vaultstadio.domain.auth.repository.SessionRepository
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.infrastructure.persistence.entities.UserSessionsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

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

    override suspend fun rotate(
        session: UserSession,
        expectedRefreshTokenHash: String,
    ): Either<StorageException, Boolean> = try {
        dbQuery {
            UserSessionsTable.update({
                (UserSessionsTable.id eq session.id) and
                    (UserSessionsTable.refreshTokenHash eq expectedRefreshTokenHash)
            }) {
                it[tokenHash] = session.tokenHash
                it[refreshTokenHash] = session.refreshTokenHash
                it[expiresAt] = session.expiresAt
                it[lastActivityAt] = session.lastActivityAt
            } == 1
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to rotate session", e).left()
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
