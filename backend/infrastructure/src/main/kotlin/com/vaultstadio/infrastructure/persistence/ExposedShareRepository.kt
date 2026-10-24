/**
 * VaultStadio Share Repository Implementation
 */

package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.activity.model.Activity
import com.vaultstadio.domain.activity.model.ActivityType
import com.vaultstadio.domain.activity.model.StorageStatistics
import com.vaultstadio.domain.activity.repository.ActivityQuery
import com.vaultstadio.domain.activity.repository.ActivityRepository
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.domain.share.repository.ShareRepository
import com.vaultstadio.infrastructure.persistence.entities.ActivitiesTable
import com.vaultstadio.infrastructure.persistence.entities.ShareLinksTable
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

private val logger = KotlinLogging.logger {}

class ExposedShareRepository : ShareRepository {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

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
    } catch (e: Exception) {
        DatabaseException("Failed to create share", e).left()
    }

    override suspend fun findById(id: String): Either<StorageException, ShareLink?> = try {
        dbQuery {
            ShareLinksTable.selectAll()
                .where { ShareLinksTable.id eq id }
                .map { it.toShareLink() }
                .singleOrNull()
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find share", e).left()
    }

    override suspend fun findByToken(token: String): Either<StorageException, ShareLink?> = try {
        dbQuery {
            ShareLinksTable.selectAll()
                .where { ShareLinksTable.token eq token }
                .map { it.toShareLink() }
                .singleOrNull()
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find share by token", e).left()
    }

    override suspend fun findByItemId(itemId: String): Either<StorageException, List<ShareLink>> = try {
        dbQuery {
            ShareLinksTable.selectAll()
                .where { ShareLinksTable.itemId eq itemId }
                .map { it.toShareLink() }
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find shares by item", e).left()
    }

    override suspend fun findByCreatedBy(
        userId: String,
        activeOnly: Boolean,
    ): Either<StorageException, List<ShareLink>> = try {
        dbQuery {
            val query = if (activeOnly) {
                ShareLinksTable.selectAll()
                    .where { (ShareLinksTable.createdBy eq userId) and (ShareLinksTable.isActive eq true) }
            } else {
                ShareLinksTable.selectAll()
                    .where { ShareLinksTable.createdBy eq userId }
            }
            query.map { it.toShareLink() }
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find shares by creator", e).left()
    }

    override suspend fun update(share: ShareLink): Either<StorageException, ShareLink> = try {
        dbQuery {
            ShareLinksTable.update({ ShareLinksTable.id eq share.id }) {
                it[expiresAt] = share.expiresAt
                it[password] = share.password
                it[maxDownloads] = share.maxDownloads
                it[downloadCount] = share.downloadCount
                it[isActive] = share.isActive
            }
        }
        share.right()
    } catch (e: Exception) {
        DatabaseException("Failed to update share", e).left()
    }

    override suspend fun incrementDownloadCount(id: String): Either<StorageException, Int> = try {
        dbQuery {
            val current = ShareLinksTable.selectAll()
                .where { ShareLinksTable.id eq id }
                .map { it[ShareLinksTable.downloadCount] }
                .single()

            val newCount = current + 1
            ShareLinksTable.update({ ShareLinksTable.id eq id }) {
                it[downloadCount] = newCount
            }
            newCount
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to increment download count", e).left()
    }

    override suspend fun delete(id: String): Either<StorageException, Unit> = try {
        dbQuery {
            ShareLinksTable.deleteWhere { ShareLinksTable.id eq id }
        }
        Unit.right()
    } catch (e: Exception) {
        DatabaseException("Failed to delete share", e).left()
    }

    override suspend fun deleteByItemId(itemId: String): Either<StorageException, Int> = try {
        dbQuery {
            ShareLinksTable.deleteWhere { ShareLinksTable.itemId eq itemId }
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to delete shares by item", e).left()
    }

    override suspend fun deactivateExpired(before: Instant): Either<StorageException, Int> = try {
        dbQuery {
            ShareLinksTable.update({
                (ShareLinksTable.expiresAt lessEq before) and (ShareLinksTable.isActive eq true)
            }) {
                it[isActive] = false
            }
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to deactivate expired shares", e).left()
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
            query.map { it.toShareLink() }
                .filter { share ->
                    // Check if user is in the sharedWithUsers list
                    share.sharedWithUsers.contains(userId)
                }
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to find shares shared with user", e).left()
    }

    private fun ResultRow.toShareLink(): ShareLink {
        val sharedWithStr = this[ShareLinksTable.sharedWithUsers]
        val sharedWithList = if (sharedWithStr.isBlank()) {
            emptyList()
        } else {
            sharedWithStr.split(",").filter { it.isNotBlank() }
        }
        return ShareLink(
            id = this[ShareLinksTable.id],
            itemId = this[ShareLinksTable.itemId],
            token = this[ShareLinksTable.token],
            createdBy = this[ShareLinksTable.createdBy],
            expiresAt = this[ShareLinksTable.expiresAt],
            password = this[ShareLinksTable.password],
            maxDownloads = this[ShareLinksTable.maxDownloads],
            downloadCount = this[ShareLinksTable.downloadCount],
            isActive = this[ShareLinksTable.isActive],
            createdAt = this[ShareLinksTable.createdAt],
            sharedWithUsers = sharedWithList,
        )
    }
}

class ExposedActivityRepository : ActivityRepository {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(activity: Activity): Either<StorageException, Activity> = try {
        dbQuery {
            ActivitiesTable.insert {
                it[id] = activity.id
                it[type] = activity.type.name
                it[userId] = activity.userId
                it[itemId] = activity.itemId
                it[itemPath] = activity.itemPath
                it[details] = activity.details
                it[ipAddress] = activity.ipAddress
                it[userAgent] = activity.userAgent
                it[createdAt] = activity.createdAt
            }
        }
        activity.right()
    } catch (e: Exception) {
        DatabaseException("Failed to create activity", e).left()
    }

    override suspend fun query(query: ActivityQuery): Either<StorageException, PagedResult<Activity>> = try {
        dbQuery {
            val total = ActivitiesTable.selectAll().count()
            val items = ActivitiesTable.selectAll()
                .limit(query.limit)
                .offset(query.offset.toLong())
                .map { it.toActivity() }
            PagedResult(items, total, query.offset, query.limit)
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to query activities", e).left()
    }

    override suspend fun getRecentByUser(userId: String, limit: Int): Either<StorageException, List<Activity>> = try {
        dbQuery {
            ActivitiesTable.selectAll()
                .where { ActivitiesTable.userId eq userId }
                .orderBy(ActivitiesTable.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
                .limit(limit)
                .map { it.toActivity() }
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to get recent activities", e).left()
    }

    override suspend fun getRecentByItem(itemId: String, limit: Int): Either<StorageException, List<Activity>> = try {
        dbQuery {
            ActivitiesTable.selectAll()
                .where { ActivitiesTable.itemId eq itemId }
                .orderBy(ActivitiesTable.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
                .limit(limit)
                .map { it.toActivity() }
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to get recent activities", e).left()
    }

    override suspend fun deleteOlderThan(before: Instant): Either<StorageException, Int> = try {
        dbQuery {
            ActivitiesTable.deleteWhere { ActivitiesTable.createdAt lessEq before }
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to delete old activities", e).left()
    }

    override suspend fun getStatistics(): Either<StorageException, StorageStatistics> = try {
        dbQuery {
            StorageStatistics(
                totalFiles = 0L,
                totalFolders = 0L,
                totalSize = 0L,
                totalUsers = 0L,
                activeUsers = 0L,
                totalShares = 0L,
                uploadsToday = 0L,
                downloadsToday = 0L,
                collectedAt = kotlinx.datetime.Clock.System.now(),
            )
        }.right()
    } catch (e: Exception) {
        DatabaseException("Failed to get statistics", e).left()
    }

    private fun ResultRow.toActivity() = Activity(
        id = this[ActivitiesTable.id],
        type = ActivityType.valueOf(this[ActivitiesTable.type]),
        userId = this[ActivitiesTable.userId],
        itemId = this[ActivitiesTable.itemId],
        itemPath = this[ActivitiesTable.itemPath],
        details = this[ActivitiesTable.details],
        ipAddress = this[ActivitiesTable.ipAddress],
        userAgent = this[ActivitiesTable.userAgent],
        createdAt = this[ActivitiesTable.createdAt],
    )
}
