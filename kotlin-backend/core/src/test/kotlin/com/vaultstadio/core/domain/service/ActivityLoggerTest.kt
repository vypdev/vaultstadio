/**
 * VaultStadio Activity Logger Tests
 *
 * Unit tests for ActivityLogger: start subscribes to events, logging calls repository, stop unsubscribes.
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.event.FileEvent
import com.vaultstadio.core.domain.event.FolderEvent
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

    @Test
    fun `start then publish FileEvent Downloaded calls activityRepository create with FILE_DOWNLOADED`() = runTest {
        activityLogger.start()
        val item = StorageItem(
            id = "item-2",
            name = "doc.pdf",
            path = "/doc.pdf",
            type = ItemType.FILE,
            ownerId = "user-1",
            size = 200,
            mimeType = "application/pdf",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )
        val event = FileEvent.Downloaded(
            id = "ev-dl",
            timestamp = Clock.System.now(),
            userId = "user-1",
            item = item,
            accessedViaShare = false,
        )
        eventBus.publish(event, async = false)
        coVerify(exactly = 1) {
            activityRepository.create(match { it.type == ActivityType.FILE_DOWNLOADED && it.itemId == "item-2" })
        }
    }

    @Test
    fun `start then publish FolderEvent Created calls activityRepository create with FOLDER_CREATED`() = runTest {
        activityLogger.start()
        val folder = StorageItem(
            id = "folder-1",
            name = "Documents",
            path = "/Documents",
            type = ItemType.FOLDER,
            ownerId = "user-1",
            size = 0,
            mimeType = null,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )
        val event = FolderEvent.Created(
            id = "ev-folder",
            timestamp = Clock.System.now(),
            userId = "user-1",
            folder = folder,
        )
        eventBus.publish(event, async = false)
        coVerify(exactly = 1) {
            activityRepository.create(match { it.type == ActivityType.FOLDER_CREATED && it.itemId == "folder-1" })
        }
    }

    @Test
    fun `start then publish FileEvent Moved calls activityRepository create with FILE_MOVED`() = runTest {
        activityLogger.start()
        val item = StorageItem(
            id = "item-moved",
            name = "doc.txt",
            path = "/new/doc.txt",
            type = ItemType.FILE,
            ownerId = "user-1",
            size = 50,
            mimeType = "text/plain",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )
        val event = FileEvent.Moved(
            id = "ev-moved",
            timestamp = Clock.System.now(),
            userId = "user-1",
            item = item,
            previousPath = "/old/doc.txt",
            previousParentId = "parent-old",
        )
        eventBus.publish(event, async = false)
        coVerify(exactly = 1) {
            activityRepository.create(
                match {
                    it.type == ActivityType.FILE_MOVED && it.itemId == "item-moved" &&
                        it.details != null && it.details.contains("old/doc.txt")
                },
            )
        }
    }

    @Test
    fun `start then publish FolderEvent Deleted calls activityRepository create with FOLDER_DELETED`() = runTest {
        activityLogger.start()
        val folder = StorageItem(
            id = "folder-del",
            name = "Trash",
            path = "/Trash",
            type = ItemType.FOLDER,
            ownerId = "user-1",
            size = 0,
            mimeType = null,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )
        val event = FolderEvent.Deleted(
            id = "ev-fdel",
            timestamp = Clock.System.now(),
            userId = "user-1",
            folder = folder,
            itemCount = 3,
        )
        eventBus.publish(event, async = false)
        coVerify(exactly = 1) {
            activityRepository.create(
                match {
                    it.type == ActivityType.FOLDER_DELETED && it.itemId == "folder-del" &&
                        it.details != null && it.details.contains("3")
                },
            )
        }
    }
}
