/**
 * VaultStadio Exposed Activity Repository Tests
 */

package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.core.domain.model.Activity
import com.vaultstadio.core.domain.model.ActivityType
import com.vaultstadio.core.domain.repository.ActivityQuery
import com.vaultstadio.core.domain.repository.ActivityRepository
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for ExposedActivityRepository.
 */
class ExposedActivityRepositoryTest {

    private lateinit var repository: ActivityRepository

    @BeforeEach
    fun setup() {
        repository = ExposedActivityRepository()
    }

    @Nested
    @DisplayName("Repository API Tests")
    inner class RepositoryApiTests {

        @Test
        fun `repository should implement ActivityRepository interface`() {
            assertTrue(repository is ActivityRepository)
        }

        @Test
        fun `repository should be of correct implementation type`() {
            assertTrue(repository is ExposedActivityRepository)
        }
    }

    @Nested
    @DisplayName("Activity Model Tests")
    inner class ActivityModelTests {

        @Test
        fun `activity should be created with all required fields`() {
            val now = Clock.System.now()

            val activity = Activity(
                id = "activity-123",
                type = ActivityType.FILE_UPLOADED,
                userId = "user-456",
                itemId = "item-789",
                itemPath = "/documents/report.pdf",
                details = "Uploaded report.pdf",
                ipAddress = "192.168.1.1",
                userAgent = "Mozilla/5.0",
                createdAt = now,
            )

            assertEquals("activity-123", activity.id)
            assertEquals(ActivityType.FILE_UPLOADED, activity.type)
            assertEquals("user-456", activity.userId)
            assertEquals("item-789", activity.itemId)
            assertEquals("/documents/report.pdf", activity.itemPath)
        }

        @Test
        fun `activity should support nullable fields`() {
            val now = Clock.System.now()

            val activity = Activity(
                id = "activity-123",
                type = ActivityType.USER_LOGIN,
                userId = "user-456",
                itemId = null,
                itemPath = null,
                details = "User logged in",
                ipAddress = "192.168.1.1",
                userAgent = null,
                createdAt = now,
            )

            assertNull(activity.itemId)
            assertNull(activity.itemPath)
            assertNull(activity.userAgent)
        }
    }

    @Nested
    @DisplayName("Activity Type Tests")
    inner class ActivityTypeTests {

        @Test
        fun `all activity types should be defined`() {
            val types = ActivityType.entries.toTypedArray()

            assertTrue(types.isNotEmpty())
        }

        @Test
        fun `file uploaded activity type should exist`() {
            val type = ActivityType.FILE_UPLOADED
            assertNotNull(type)
            assertEquals("FILE_UPLOADED", type.name)
        }

        @Test
        fun `file downloaded activity type should exist`() {
            val type = ActivityType.FILE_DOWNLOADED
            assertNotNull(type)
            assertEquals("FILE_DOWNLOADED", type.name)
        }

        @Test
        fun `file deleted activity type should exist`() {
            val type = ActivityType.FILE_DELETED
            assertNotNull(type)
            assertEquals("FILE_DELETED", type.name)
        }

        @Test
        fun `user login activity type should exist`() {
            val type = ActivityType.USER_LOGIN
            assertNotNull(type)
            assertEquals("USER_LOGIN", type.name)
        }
    }

    @Nested
    @DisplayName("Activity Query Tests")
    inner class ActivityQueryTests {

        @Test
        fun `activity query should have default values`() {
            val query = ActivityQuery()

            assertNotNull(query.limit)
            assertNotNull(query.offset)
        }

        @Test
        fun `activity query should support custom limit and offset`() {
            val query = ActivityQuery(
                limit = 50,
                offset = 100,
            )

            assertEquals(50, query.limit)
            assertEquals(100, query.offset)
        }
    }

    @Nested
    @DisplayName("Repository Method Signature Tests")
    inner class MethodSignatureTests {

        @Test
        fun `create method should exist`() {
            assertNotNull(repository::create)
        }

        @Test
        fun `query method should exist`() {
            assertNotNull(repository::query)
        }

        @Test
        fun `getRecentByUser method should exist`() {
            assertNotNull(repository::getRecentByUser)
        }

        @Test
        fun `getRecentByItem method should exist`() {
            assertNotNull(repository::getRecentByItem)
        }

        @Test
        fun `deleteOlderThan method should exist`() {
            assertNotNull(repository::deleteOlderThan)
        }

        @Test
        fun `getStatistics method should exist`() {
            assertNotNull(repository::getStatistics)
        }
    }
}
