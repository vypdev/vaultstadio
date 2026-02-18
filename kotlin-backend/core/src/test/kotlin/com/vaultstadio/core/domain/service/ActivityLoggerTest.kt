/**
 * VaultStadio Activity Logger Tests
 *
 * Unit tests for ActivityLogger: start subscribes to events, logging calls repository, stop unsubscribes.
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.event.FileEvent
import com.vaultstadio.core.domain.model.Activity
import com.vaultstadio.core.domain.model.ActivityType
import com.vaultstadio.core.domain.model.ItemType
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.repository.ActivityRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ActivityLoggerTest {

    private lateinit var eventBus: EventBus
    private lateinit var activityRepository: ActivityRepository
    private lateinit var activityLogger: ActivityLogger

    @BeforeEach
    fun setup() {
        eventBus = EventBus()
        activityRepository = mockk()
        coEvery { activityRepository.create(any()) } coAnswers { Either.Right(firstArg()) }
        activityLogger = ActivityLogger(eventBus, activityRepository)
    }

    @AfterEach
    fun teardown() {
        activityLogger.stop()
        eventBus.shutdown()
    }

    @Test
    fun `start then publish FileEvent Uploaded calls activityRepository create with FILE_UPLOADED`() = runTest {
        activityLogger.start()
        val item = StorageItem(
            id = "item-1",
            name = "test.pdf",
            path = "/test.pdf",
            type = ItemType.FILE,
            ownerId = "user-1",
            size = 100,
            mimeType = "application/pdf",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )
        val event = FileEvent.Uploaded(
            id = "ev-1",
            timestamp = Clock.System.now(),
            userId = "user-1",
            item = item,
        )
        eventBus.publish(event, async = false)
        coVerify(exactly = 1) {
            activityRepository.create(match { it.type == ActivityType.FILE_UPLOADED && it.itemId == "item-1" })
        }
    }

    @Test
    fun `stop unsubscribes from event bus`() = runTest {
        activityLogger.start()
        activityLogger.stop()
        val item = StorageItem(
            id = "item-2",
            name = "x.txt",
            path = "/x.txt",
            type = ItemType.FILE,
            ownerId = "user-2",
            size = 0,
            mimeType = "text/plain",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )
        val event = FileEvent.Uploaded(
            id = "ev-2",
            timestamp = Clock.System.now(),
            userId = "user-2",
            item = item,
        )
        eventBus.publish(event, async = false)
        coVerify(exactly = 0) { activityRepository.create(any()) }
    }
}
