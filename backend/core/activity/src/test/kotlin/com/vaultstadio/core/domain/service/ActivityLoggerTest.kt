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
import com.vaultstadio.domain.activity.model.ActivityType
import com.vaultstadio.domain.activity.repository.ActivityRepository
import com.vaultstadio.domain.common.exception.StorageBackendException
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
                    val details = it.details
                    it.type == ActivityType.FILE_MOVED &&
                        it.itemId == "item-moved" &&
                        details != null &&
                        details.contains("old/doc.txt")
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
                    val details = it.details
                    it.type == ActivityType.FOLDER_DELETED &&
                        it.itemId == "folder-del" &&
                        details != null &&
                        details.contains("3")
                },
            )
        }
    }

    @Test
    fun `start then publish FileEvent Downloaded with accessedViaShare includes shareId in details`() =
        runTest {
            activityLogger.start()
            val item = StorageItem(
                id = "item-share",
                name = "shared.pdf",
                path = "/shared.pdf",
                type = ItemType.FILE,
                ownerId = "user-1",
                size = 100,
                mimeType = "application/pdf",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            )
            val event = FileEvent.Downloaded(
                id = "ev-share",
                timestamp = Clock.System.now(),
                userId = "user-1",
                item = item,
                accessedViaShare = true,
                shareId = "share-123",
            )
            eventBus.publish(event, async = false)
            coVerify(exactly = 1) {
                activityRepository.create(
                    match {
                        it.type == ActivityType.FILE_DOWNLOADED &&
                            it.itemId == "item-share" &&
                            it.details != null &&
                            it.details!!.contains("share-123")
                    },
                )
            }
        }

    @Test
    fun `when activityRepository create returns Left logger still invokes create and does not throw`() =
        runTest {
            coEvery { activityRepository.create(any()) } returns Either.Left(
                StorageBackendException("test", "create failed"),
            )
            activityLogger.start()
            val item = StorageItem(
                id = "item-err",
                name = "x.pdf",
                path = "/x.pdf",
                type = ItemType.FILE,
                ownerId = "user-1",
                size = 0,
                mimeType = "application/pdf",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            )
            val event = FileEvent.Uploaded(
                id = "ev-err",
                timestamp = Clock.System.now(),
                userId = "user-1",
                item = item,
            )
            eventBus.publish(event, async = false)
            coVerify(exactly = 1) { activityRepository.create(any()) }
        }

    @Test
    fun `start then publish FileEvent Deleted calls activityRepository create with FILE_DELETED`() =
        runTest {
            activityLogger.start()
            val item = StorageItem(
                id = "item-del",
                name = "old.pdf",
                path = "/old.pdf",
                type = ItemType.FILE,
                ownerId = "user-1",
                size = 100,
                mimeType = "application/pdf",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            )
            val event = FileEvent.Deleted(
                id = "ev-del",
                timestamp = Clock.System.now(),
                userId = "user-1",
                item = item,
                permanent = true,
            )
            eventBus.publish(event, async = false)
            coVerify(exactly = 1) {
                activityRepository.create(
                    match {
                        it.type == ActivityType.FILE_DELETED &&
                            it.itemId == "item-del" &&
                            it.details != null &&
                            it.details!!.contains("true")
                    },
                )
            }
        }

    @Test
    fun `start then publish FileEvent Renamed calls activityRepository create with FILE_RENAMED`() =
        runTest {
            activityLogger.start()
            val item = StorageItem(
                id = "item-renamed",
                name = "new-name.txt",
                path = "/new-name.txt",
                type = ItemType.FILE,
                ownerId = "user-1",
                size = 50,
                mimeType = "text/plain",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            )
            val event = FileEvent.Renamed(
                id = "ev-renamed",
                timestamp = Clock.System.now(),
                userId = "user-1",
                item = item,
                previousName = "old-name.txt",
            )
            eventBus.publish(event, async = false)
            coVerify(exactly = 1) {
                activityRepository.create(
                    match {
                        it.type == ActivityType.FILE_RENAMED &&
                            it.itemId == "item-renamed" &&
                            it.details != null &&
                            it.details!!.contains("old-name.txt") &&
                            it.details!!.contains("new-name.txt")
                    },
                )
            }
        }

    @Test
    fun `start then publish FileEvent Restored calls activityRepository create with FILE_RESTORED`() =
        runTest {
            activityLogger.start()
            val item = StorageItem(
                id = "item-restored",
                name = "doc.pdf",
                path = "/doc.pdf",
                type = ItemType.FILE,
                ownerId = "user-1",
                size = 100,
                mimeType = "application/pdf",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            )
            val event = FileEvent.Restored(
                id = "ev-restored",
                timestamp = Clock.System.now(),
                userId = "user-1",
                item = item,
            )
            eventBus.publish(event, async = false)
            coVerify(exactly = 1) {
                activityRepository.create(
                    match {
                        it.type == ActivityType.FILE_RESTORED &&
                            it.itemId == "item-restored" &&
                            it.itemPath == "/doc.pdf"
                    },
                )
            }
        }

    @Test
    fun `start then publish FileEvent Copied calls activityRepository create with FILE_COPIED`() =
        runTest {
            activityLogger.start()
            val sourceItem = StorageItem(
                id = "source-1",
                name = "original.pdf",
                path = "/original.pdf",
                type = ItemType.FILE,
                ownerId = "user-1",
                size = 200,
                mimeType = "application/pdf",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            )
            val item = StorageItem(
                id = "copy-1",
                name = "copy.pdf",
                path = "/copy.pdf",
                type = ItemType.FILE,
                ownerId = "user-1",
                size = 200,
                mimeType = "application/pdf",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            )
            val event = FileEvent.Copied(
                id = "ev-copied",
                timestamp = Clock.System.now(),
                userId = "user-1",
                item = item,
                sourceItem = sourceItem,
            )
            eventBus.publish(event, async = false)
            coVerify(exactly = 1) {
                activityRepository.create(
                    match {
                        it.type == ActivityType.FILE_COPIED &&
                            it.itemId == "copy-1" &&
                            it.details != null &&
                            it.details!!.contains("source-1") &&
                            it.details!!.contains("original.pdf")
                    },
                )
            }
        }
}
