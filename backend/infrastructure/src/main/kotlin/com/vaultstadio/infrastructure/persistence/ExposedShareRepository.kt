/**
 * VaultStadio Share Repository Implementation
 */

package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.domain.share.repository.ShareRepository
import com.vaultstadio.infrastructure.persistence.entities.ShareLinksTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import java.sql.SQLException

class ExposedShareRepository : ShareRepository {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) {
            maxAttempts = 1
            try {
                block()
            } catch (_: SQLException) {
                rollback()
                throw DatabaseException("Share persistence is unavailable")
            } catch (_: IllegalArgumentException) {
                rollback()
                throw DatabaseException("Share data is invalid")
            }
        }

    override suspend fun create(share: ShareLink): Either<StorageException, ShareLink> = try {
        dbQuery {
            ShareLinksTable.insert {
                it[id] = share.id
                it[itemId] = share.itemId
                it[token] = share.token
                it[createdBy] = share.createdBy
                it[expiresAt] = share.expiresAt
                it[password] = share.password
                it[maxDownloads] = share.maxDownloads
                it[downloadCount] = share.downloadCount
                it[isActive] = share.isActive
                it[createdAt] = share.createdAt
                it[sharedWithUsers] = share.sharedWithUsers.joinToString(",")
            }
        }
        share.right()
    } catch (error: DatabaseException) {
        error.left()
    } catch (_: SQLException) {
        DatabaseException("Failed to create share").left()
    } catch (_: IllegalArgumentException) {
        DatabaseException("Failed to create share").left()
    }

    override suspend fun findById(id: String): Either<StorageException, ShareLink?> = try {
        dbQuery {
            ShareLinksTable.selectAll()
                .where { ShareLinksTable.id eq id }
                .map { it.toShareLinkModel() }
                .singleOrNull()
        }.right()
    } catch (error: DatabaseException) {
        error.left()
    } catch (_: SQLException) {
        DatabaseException("Failed to find share").left()
    } catch (_: IllegalArgumentException) {
        DatabaseException("Failed to find share").left()
    }

    override suspend fun findByToken(token: String): Either<StorageException, ShareLink?> = try {
        dbQuery {
            ShareLinksTable.selectAll()
                .where { ShareLinksTable.token eq token }
                .map { it.toShareLinkModel() }
                .singleOrNull()
        }.right()
    } catch (error: DatabaseException) {
        error.left()
    } catch (_: SQLException) {
        DatabaseException("Failed to find share by token").left()
    } catch (_: IllegalArgumentException) {
        DatabaseException("Failed to find share by token").left()
    }

    override suspend fun findByItemId(itemId: String): Either<StorageException, List<ShareLink>> = try {
        dbQuery {
            ShareLinksTable.selectAll()
                .where { ShareLinksTable.itemId eq itemId }
                .map { it.toShareLinkModel() }
        }.right()
    } catch (error: DatabaseException) {
        error.left()
    } catch (_: SQLException) {
        DatabaseException("Failed to find shares by item").left()
    } catch (_: IllegalArgumentException) {
        DatabaseException("Failed to find shares by item").left()
    }

    override suspend fun findByCreatedBy(
        userId: String,
        activeOnly: Boolean,
    ): Either<StorageException, List<ShareLink>> = try {
        dbQuery {
            val query = if (activeOnly) {
                ShareLinksTable.selectAll()
                    .where { ShareLinksTable.createdBy eq userId and (ShareLinksTable.isActive eq true) }
            } else {
                ShareLinksTable.selectAll()
                    .where { ShareLinksTable.createdBy eq userId }
            }
            query.map { it.toShareLinkModel() }
        }.right()
    } catch (error: DatabaseException) {
        error.left()
    } catch (_: SQLException) {
        DatabaseException("Failed to find shares by creator").left()
    } catch (_: IllegalArgumentException) {
        DatabaseException("Failed to find shares by creator").left()
    }

    override suspend fun update(share: ShareLink): Either<StorageException, ShareLink> = try {
        dbQuery {
            ShareLinksTable.update({ ShareLinksTable.id eq share.id }) {
                it[expiresAt] = share.expiresAt
                it[password] = share.password
                it[maxDownloads] = share.maxDownloads
                it[isActive] = share.isActive
            }
        }
        findById(share.id).map { checkNotNull(it) }
    } catch (error: DatabaseException) {
        error.left()
    } catch (_: SQLException) {
        DatabaseException("Failed to update share").left()
    } catch (_: IllegalArgumentException) {
        DatabaseException("Failed to update share").left()
    }

    override suspend fun delete(id: String): Either<StorageException, Unit> = try {
        dbQuery {
            ShareLinksTable.deleteWhere { ShareLinksTable.id eq id }
        }
        Unit.right()
    } catch (error: DatabaseException) {
        error.left()
    } catch (_: SQLException) {
        DatabaseException("Failed to delete share").left()
    } catch (_: IllegalArgumentException) {
        DatabaseException("Failed to delete share").left()
    }

    override suspend fun deleteByItemId(itemId: String): Either<StorageException, Int> = try {
        dbQuery {
            ShareLinksTable.deleteWhere { ShareLinksTable.itemId eq itemId }
        }.right()
    } catch (error: DatabaseException) {
        error.left()
    } catch (_: SQLException) {
        DatabaseException("Failed to delete shares by item").left()
    } catch (_: IllegalArgumentException) {
        DatabaseException("Failed to delete shares by item").left()
    }

    override suspend fun deactivateExpired(before: Instant): Either<StorageException, Int> = try {
        dbQuery {
            ShareLinksTable.update({
                ShareLinksTable.expiresAt lessEq before and (ShareLinksTable.isActive eq true)
            }) {
                it[isActive] = false
            }
        }.right()
    } catch (error: DatabaseException) {
        error.left()
    } catch (_: SQLException) {
        DatabaseException("Failed to deactivate expired shares").left()
    } catch (_: IllegalArgumentException) {
        DatabaseException("Failed to deactivate expired shares").left()
    }

    override suspend fun findSharedWithUser(
        userId: String,
        activeOnly: Boolean,
    ): Either<StorageException, List<ShareLink>> = try {
        dbQuery {
            val query = if (activeOnly) {
                ShareLinksTable.selectAll()
                    .where { ShareLinksTable.isActive eq true }
            } else {
                ShareLinksTable.selectAll()
            }
            query.map { it.toShareLinkModel() }
                .filter { share ->
                    // Check if user is in the sharedWithUsers list
                    share.sharedWithUsers.contains(userId)
                }
        }.right()
    } catch (error: DatabaseException) {
        error.left()
    } catch (_: SQLException) {
        DatabaseException("Failed to find shares shared with user").left()
    } catch (_: IllegalArgumentException) {
        DatabaseException("Failed to find shares shared with user").left()
    }
}
