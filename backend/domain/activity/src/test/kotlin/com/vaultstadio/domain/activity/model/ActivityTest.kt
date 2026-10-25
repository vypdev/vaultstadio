/**
 * Unit tests for [Activity], [ActivityType], [StorageStatistics].
 */

package com.vaultstadio.domain.activity.model

import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ActivityTest {

    private val now = Instant.fromEpochMilliseconds(1_600_000_000_000)

    @Test
    fun activityHoldsTypeAndOptionalItemInfo() {
        val activity = Activity(
            id = "a1",
            type = ActivityType.FILE_UPLOADED,
            userId = "user-1",
            itemId = "item-1",
            itemPath = "/file.txt",
            details = null,
            ipAddress = null,
            userAgent = null,
            createdAt = now,
        )
        assertEquals(ActivityType.FILE_UPLOADED, activity.type)
        assertEquals("user-1", activity.userId)
        assertEquals("item-1", activity.itemId)
        assertEquals("/file.txt", activity.itemPath)
        assertNull(activity.details)
    }

    @Test
    fun activityTypeEnumValues() {
        assertEquals(ActivityType.FILE_UPLOADED, ActivityType.entries.first { it == ActivityType.FILE_UPLOADED })
        assertEquals(ActivityType.SHARE_CREATED, ActivityType.entries.first { it == ActivityType.SHARE_CREATED })
    }

    @Test
    fun storageStatisticsHoldsCounts() {
        val stats = StorageStatistics(
            totalFiles = 100,
            totalFolders = 20,
            totalSize = 1_000_000,
            totalUsers = 5,
            activeUsers = 3,
            totalShares = 10,
            uploadsToday = 2,
            downloadsToday = 5,
            collectedAt = now,
        )
        assertEquals(100, stats.totalFiles)
        assertEquals(20, stats.totalFolders)
        assertEquals(5, stats.totalUsers)
        assertEquals(now, stats.collectedAt)
    }
}
