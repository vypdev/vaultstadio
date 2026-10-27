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
import com.vaultstadio.infrastructure.persistence.entities.ActivitiesTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.sql.SQLException

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
    } catch (_: SQLException) {
        DatabaseException("Failed to create activity").left()
    } catch (_: IllegalArgumentException) {
        DatabaseException("Failed to create activity").left()
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
    } catch (_: SQLException) {
        DatabaseException("Failed to query activities").left()
    } catch (_: IllegalArgumentException) {
        DatabaseException("Failed to query activities").left()
    }

    override suspend fun getRecentByUser(userId: String, limit: Int): Either<StorageException, List<Activity>> = try {
        dbQuery {
            ActivitiesTable.selectAll()
                .where { ActivitiesTable.userId eq userId }
                .orderBy(ActivitiesTable.createdAt, SortOrder.DESC)
                .limit(limit)
                .map { it.toActivity() }
        }.right()
    } catch (_: SQLException) {
        DatabaseException("Failed to get recent activities").left()
    } catch (_: IllegalArgumentException) {
        DatabaseException("Failed to get recent activities").left()
    }

    override suspend fun getRecentByItem(itemId: String, limit: Int): Either<StorageException, List<Activity>> = try {
        dbQuery {
            ActivitiesTable.selectAll()
                .where { ActivitiesTable.itemId eq itemId }
                .orderBy(ActivitiesTable.createdAt, SortOrder.DESC)
                .limit(limit)
                .map { it.toActivity() }
        }.right()
    } catch (_: SQLException) {
        DatabaseException("Failed to get recent activities").left()
    } catch (_: IllegalArgumentException) {
        DatabaseException("Failed to get recent activities").left()
    }

    override suspend fun deleteOlderThan(before: Instant): Either<StorageException, Int> = try {
        dbQuery {
            ActivitiesTable.deleteWhere { ActivitiesTable.createdAt lessEq before }
        }.right()
    } catch (_: SQLException) {
        DatabaseException("Failed to delete old activities").left()
    } catch (_: IllegalArgumentException) {
        DatabaseException("Failed to delete old activities").left()
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
                collectedAt = Clock.System.now(),
            )
        }.right()
    } catch (_: SQLException) {
        DatabaseException("Failed to get statistics").left()
    } catch (_: IllegalArgumentException) {
        DatabaseException("Failed to get statistics").left()
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
