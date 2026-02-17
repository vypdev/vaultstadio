/**
 * VaultStadio Storage Service Tests
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.event.FileEvent
import com.vaultstadio.core.domain.event.FolderEvent
import com.vaultstadio.core.domain.model.ItemType
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.model.Visibility
import com.vaultstadio.core.domain.repository.PagedResult
import com.vaultstadio.core.domain.repository.StorageItemRepository
import com.vaultstadio.core.exception.AuthorizationException
import com.vaultstadio.core.exception.ItemNotFoundException
import com.vaultstadio.core.exception.ValidationException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StorageServiceTest {

    private lateinit var storageItemRepository: StorageItemRepository
    private lateinit var storageBackend: StorageBackend
    private lateinit var eventBus: EventBus
    private lateinit var storageService: StorageService

    @BeforeEach
    fun setup() {
        storageItemRepository = mockk()
        storageBackend = mockk()
        eventBus = mockk(relaxed = true)
        storageService = StorageService(storageItemRepository, storageBackend, eventBus)
    }

    @Nested
    inner class CreateFolderTests {

        @Test
        fun `should create folder successfully at root`() = runTest {
            // Given
            val input = CreateFolderInput(
                name = "Documents",
                parentId = null,
                ownerId = "user-123",
            )

            coEvery {
                storageItemRepository.existsByPath("/Documents", "user-123")
            } returns false.right()

            coEvery {
                storageItemRepository.create(any())
            } answers {
                firstArg<StorageItem>().right()
            }

            // When
            val result = storageService.createFolder(input)

            // Then
            assertTrue(result.isRight())
            val folder = (result as Either.Right).value
            assertEquals("Documents", folder.name)
            assertEquals("/Documents", folder.path)
            assertEquals(ItemType.FOLDER, folder.type)
            assertEquals("user-123", folder.ownerId)

            coVerify { eventBus.publish(match<FolderEvent.Created> { it.folder.name == "Documents" }) }
        }

        @Test
        fun `should create folder in parent folder`() = runTest {
            // Given
            val parentFolder = createTestFolder("parent-id", "Parent", "/Parent", "user-123")
            val input = CreateFolderInput(
                name = "Child",
                parentId = "parent-id",
                ownerId = "user-123",
            )

            coEvery {
                storageItemRepository.findById("parent-id")
            } returns parentFolder.right()

            coEvery {
                storageItemRepository.existsByPath("/Parent/Child", "user-123")
            } returns false.right()

            coEvery {
                storageItemRepository.create(any())
            } answers {
                firstArg<StorageItem>().right()
            }

            // When
            val result = storageService.createFolder(input)

            // Then
            assertTrue(result.isRight())
            val folder = (result as Either.Right).value
            assertEquals("Child", folder.name)
            assertEquals("/Parent/Child", folder.path)
            assertEquals("parent-id", folder.parentId)
        }

        @Test
        fun `should fail with empty name`() = runTest {
            // Given
            val input = CreateFolderInput(
                name = "",
                parentId = null,
                ownerId = "user-123",
            )

            // When
            val result = storageService.createFolder(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ValidationException)
        }

        @Test
        fun `should fail when folder already exists`() = runTest {
            // Given
            val input = CreateFolderInput(
                name = "Existing",
                parentId = null,
                ownerId = "user-123",
            )

            coEvery {
                storageItemRepository.existsByPath("/Existing", "user-123")
            } returns true.right()

            // When
            val result = storageService.createFolder(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ValidationException)
        }

        @Test
        fun `should fail when parent is not a folder`() = runTest {
            // Given
            val parentFile = createTestFile("parent-id", "file.txt", "/file.txt", "user-123")
            val input = CreateFolderInput(
                name = "Child",
                parentId = "parent-id",
                ownerId = "user-123",
            )

            coEvery {
                storageItemRepository.findById("parent-id")
            } returns parentFile.right()

            // When
            val result = storageService.createFolder(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ValidationException)
        }
    }

    @Nested
    inner class UploadFileTests {

        @Test
        fun `should upload file successfully`() = runTest {
            // Given
            val content = "Hello, World!"
            val inputStream = ByteArrayInputStream(content.toByteArray())
            val input = UploadFileInput(
                name = "test.txt",
                parentId = null,
                ownerId = "user-123",
                mimeType = "text/plain",
                size = content.length.toLong(),
                inputStream = inputStream,
            )

            coEvery {
                storageItemRepository.existsByPath(any(), any())
            } returns false.right()

            coEvery {
                storageBackend.store(any(), any(), any())
            } returns "storage-key-123".right()

            coEvery {
                storageItemRepository.create(any())
            } answers {
                firstArg<StorageItem>().right()
            }

            // When
            val result = storageService.uploadFile(input)

            // Then
            assertTrue(result.isRight())
            val file = (result as Either.Right).value
            assertEquals("test.txt", file.name)
            assertEquals("/test.txt", file.path)
            assertEquals(ItemType.FILE, file.type)
            assertEquals("text/plain", file.mimeType)
            assertEquals("storage-key-123", file.storageKey)

            coVerify { eventBus.publish(match<FileEvent.Uploaded> { it.item.name == "test.txt" }) }
        }

        @Test
        fun `should fail with empty name`() = runTest {
            // Given
            val input = UploadFileInput(
                name = "",
                parentId = null,
                ownerId = "user-123",
                mimeType = "text/plain",
                size = 100,
                inputStream = ByteArrayInputStream(ByteArray(0)),
            )

            // When
            val result = storageService.uploadFile(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ValidationException)
        }
    }

    @Nested
    inner class GetItemTests {

        @Test
        fun `should get item successfully`() = runTest {
            // Given
            val item = createTestFile("file-id", "test.txt", "/test.txt", "user-123")

            coEvery {
                storageItemRepository.findById("file-id")
            } returns item.right()

            // When
            val result = storageService.getItem("file-id", "user-123")

            // Then
            assertTrue(result.isRight())
            assertEquals(item.id, (result as Either.Right).value.id)
        }

        @Test
        fun `should fail when item not found`() = runTest {
            // Given
            coEvery {
                storageItemRepository.findById("missing-id")
            } returns null.right()

            // When
            val result = storageService.getItem("missing-id", "user-123")

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ItemNotFoundException)
        }

        @Test
        fun `should fail when user is not owner`() = runTest {
            // Given
            val item = createTestFile("file-id", "test.txt", "/test.txt", "other-user")

            coEvery {
                storageItemRepository.findById("file-id")
            } returns item.right()

            // When
            val result = storageService.getItem("file-id", "user-123")

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is AuthorizationException)
        }
    }

    @Nested
    inner class RenameItemTests {

        @Test
        fun `should rename item successfully`() = runTest {
            // Given
            val item = createTestFile("file-id", "old.txt", "/old.txt", "user-123")

            coEvery {
                storageItemRepository.findById("file-id")
            } returns item.right()

            coEvery {
                storageItemRepository.update(any())
            } answers {
                firstArg<StorageItem>().right()
            }

            // When
            val result = storageService.renameItem("file-id", "new.txt", "user-123")

            // Then
            assertTrue(result.isRight())
            val renamed = (result as Either.Right).value
            assertEquals("new.txt", renamed.name)
            assertEquals("/new.txt", renamed.path)
        }

        @Test
        fun `should fail with empty name`() = runTest {
            // Given
            val item = createTestFile("file-id", "old.txt", "/old.txt", "user-123")

            coEvery {
                storageItemRepository.findById("file-id")
            } returns item.right()

            // When
            val result = storageService.renameItem("file-id", "  ", "user-123")

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ValidationException)
        }
    }

    @Nested
    inner class ToggleStarTests {

        @Test
        fun `should star an unstarred item`() = runTest {
            // Given
            val item = createTestFile("file-id", "test.txt", "/test.txt", "user-123").copy(isStarred = false)

            coEvery {
                storageItemRepository.findById("file-id")
            } returns item.right()

            coEvery {
                storageItemRepository.update(any())
            } answers {
                firstArg<StorageItem>().right()
            }

            // When
            val result = storageService.toggleStar("file-id", "user-123")

            // Then
            assertTrue(result.isRight())
            assertEquals(true, (result as Either.Right).value.isStarred)
        }

        @Test
        fun `should unstar a starred item`() = runTest {
            // Given
            val item = createTestFile("file-id", "test.txt", "/test.txt", "user-123").copy(isStarred = true)

            coEvery {
                storageItemRepository.findById("file-id")
            } returns item.right()

            coEvery {
                storageItemRepository.update(any())
            } answers {
                firstArg<StorageItem>().right()
            }

            // When
            val result = storageService.toggleStar("file-id", "user-123")

            // Then
            assertTrue(result.isRight())
            assertEquals(false, (result as Either.Right).value.isStarred)
        }
    }

    @Nested
    inner class TrashAndRestoreTests {

        @Test
        fun `should trash item successfully`() = runTest {
            // Given
            val item = createTestFile("file-id", "test.txt", "/test.txt", "user-123")

            coEvery {
                storageItemRepository.findById("file-id")
            } returns item.right()

            coEvery {
                storageItemRepository.update(any())
            } answers {
                firstArg<StorageItem>().right()
            }

            // When
            val result = storageService.trashItem("file-id", "user-123")

            // Then
            assertTrue(result.isRight())
            val trashed = (result as Either.Right).value
            assertEquals(true, trashed.isTrashed)
            assertTrue(trashed.trashedAt != null)

            coVerify { eventBus.publish(match<FileEvent.Deleted> { !it.permanent }) }
        }

        @Test
        fun `should restore item successfully`() = runTest {
            // Given
            val item = createTestFile("file-id", "test.txt", "/test.txt", "user-123")
                .copy(isTrashed = true, trashedAt = Clock.System.now())

            coEvery {
                storageItemRepository.findById("file-id")
            } returns item.right()

            coEvery {
                storageItemRepository.update(any())
            } answers {
                firstArg<StorageItem>().right()
            }

            // When
            val result = storageService.restoreItem("file-id", "user-123")

            // Then
            assertTrue(result.isRight())
            val restored = (result as Either.Right).value
            assertEquals(false, restored.isTrashed)
            assertEquals(null, restored.trashedAt)

            coVerify { eventBus.publish(any<FileEvent.Restored>()) }
        }
    }

    @Nested
    inner class GetTrashItemsTests {

        @Test
        fun `should return trashed items for user`() = runTest {
            val userId = "user-123"
            val trashedItem = createTestFile("file-1", "trashed.txt", "/trashed.txt", userId)
                .copy(isTrashed = true, trashedAt = Clock.System.now())

            coEvery {
                storageItemRepository.query(any())
            } returns PagedResult<StorageItem>(items = listOf(trashedItem), total = 1, offset = 0, limit = 100).right()

            val result = storageService.getTrashItems(userId)

            assertTrue(result.isRight())
            val items = (result as Either.Right).value
            assertEquals(1, items.size)
            assertEquals("trashed.txt", items.first().name)
            assertTrue(items.first().isTrashed)
        }

        @Test
        fun `should return empty list when user has no trashed items`() = runTest {
            val userId = "user-456"
            coEvery {
                storageItemRepository.query(any())
            } returns PagedResult<StorageItem>(items = emptyList(), total = 0, offset = 0, limit = 100).right()

            val result = storageService.getTrashItems(userId)

            assertTrue(result.isRight())
            assertEquals(0, (result as Either.Right).value.size)
        }
    }

    @Nested
    inner class GetStarredItemsTests {

        @Test
        fun `should return starred items for user`() = runTest {
            val userId = "user-123"
            val starredItem = createTestFile("file-1", "starred.txt", "/starred.txt", userId)
                .copy(isStarred = true)

            coEvery {
                storageItemRepository.query(any())
            } returns PagedResult<StorageItem>(items = listOf(starredItem), total = 1, offset = 0, limit = 100).right()

            val result = storageService.getStarredItems(userId)

            assertTrue(result.isRight())
            val items = (result as Either.Right).value
            assertEquals(1, items.size)
            assertEquals("starred.txt", items.first().name)
            assertTrue(items.first().isStarred)
        }

        @Test
        fun `should return empty list when user has no starred items`() = runTest {
            val userId = "user-456"
            coEvery {
                storageItemRepository.query(any())
            } returns PagedResult<StorageItem>(items = emptyList(), total = 0, offset = 0, limit = 100).right()

            val result = storageService.getStarredItems(userId)

            assertTrue(result.isRight())
            assertEquals(0, (result as Either.Right).value.size)
        }
    }

    @Nested
    inner class GetRecentItemsTests {

        @Test
        fun `should return recent items sorted by updatedAt desc`() = runTest {
            val userId = "user-123"
            val recentFile = createTestFile("file-1", "recent.txt", "/recent.txt", userId)

            coEvery {
                storageItemRepository.query(any())
            } returns PagedResult<StorageItem>(items = listOf(recentFile), total = 1, offset = 0, limit = 20).right()

            val result = storageService.getRecentItems(userId, limit = 20)

            assertTrue(result.isRight())
            val items = (result as Either.Right).value
            assertEquals(1, items.size)
            assertEquals("recent.txt", items.first().name)
        }

        @Test
        fun `should respect limit parameter`() = runTest {
            val userId = "user-123"
            coEvery {
                storageItemRepository.query(any())
            } returns PagedResult<StorageItem>(items = emptyList(), total = 0, offset = 0, limit = 10).right()

            val result = storageService.getRecentItems(userId, limit = 10)

            assertTrue(result.isRight())
            assertEquals(0, (result as Either.Right).value.size)
            coVerify(atLeast = 1) { storageItemRepository.query(any()) }
        }
    }

    @Nested
    inner class SetStarTests {

        @Test
        fun `should set star to true`() = runTest {
            val item = createTestFile("file-id", "test.txt", "/test.txt", "user-123").copy(isStarred = false)
            coEvery { storageItemRepository.findById("file-id") } returns item.right()
            coEvery { storageItemRepository.update(any()) } answers { firstArg<StorageItem>().right() }

            val result = storageService.setStar("file-id", "user-123", starred = true)

            assertTrue(result.isRight())
            assertEquals(true, (result as Either.Right).value.isStarred)
        }

        @Test
        fun `should set star to false`() = runTest {
            val item = createTestFile("file-id", "test.txt", "/test.txt", "user-123").copy(isStarred = true)
            coEvery { storageItemRepository.findById("file-id") } returns item.right()
            coEvery { storageItemRepository.update(any()) } answers { firstArg<StorageItem>().right() }

            val result = storageService.setStar("file-id", "user-123", starred = false)

            assertTrue(result.isRight())
            assertEquals(false, (result as Either.Right).value.isStarred)
        }

        @Test
        fun `should return same item when already in desired star state`() = runTest {
            val item = createTestFile("file-id", "test.txt", "/test.txt", "user-123").copy(isStarred = true)
            coEvery { storageItemRepository.findById("file-id") } returns item.right()

            val result = storageService.setStar("file-id", "user-123", starred = true)

            assertTrue(result.isRight())
            assertEquals(true, (result as Either.Right).value.isStarred)
            coVerify(exactly = 0) { storageItemRepository.update(any()) }
        }
    }

    @Nested
    inner class DeleteItemTests {

        @Test
        fun `should delete file permanently`() = runTest {
            // Given
            val item = createTestFile("file-id", "test.txt", "/test.txt", "user-123")

            coEvery {
                storageItemRepository.findById("file-id")
            } returns item.right()

            coEvery {
                storageBackend.delete(any())
            } returns Unit.right()

            coEvery {
                storageItemRepository.delete("file-id")
            } returns Unit.right()

            // When
            val result = storageService.deleteItem("file-id", "user-123")

            // Then
            assertTrue(result.isRight())
            coVerify { storageBackend.delete(item.storageKey!!) }
            coVerify { eventBus.publish(match<FileEvent.Deleted> { it.permanent }) }
        }

        @Test
        fun `should delete folder without storage backend call`() = runTest {
            // Given
            val folder = createTestFolder("folder-id", "Folder", "/Folder", "user-123")

            coEvery {
                storageItemRepository.findById("folder-id")
            } returns folder.right()

            coEvery {
                storageItemRepository.delete("folder-id")
            } returns Unit.right()

            // When
            val result = storageService.deleteItem("folder-id", "user-123")

            // Then
            assertTrue(result.isRight())
            coVerify(exactly = 0) { storageBackend.delete(any()) }
        }
    }

    // Helper functions to create test data

    private fun createTestFile(
        id: String,
        name: String,
        path: String,
        ownerId: String,
    ): StorageItem {
        val now = Clock.System.now()
        return StorageItem(
            id = id,
            name = name,
            path = path,
            type = ItemType.FILE,
            parentId = null,
            ownerId = ownerId,
            size = 1000,
            mimeType = "text/plain",
            checksum = "abc123",
            storageKey = "storage-$id",
            visibility = Visibility.PRIVATE,
            isTrashed = false,
            isStarred = false,
            createdAt = now,
            updatedAt = now,
            trashedAt = null,
            version = 1,
        )
    }

    private fun createTestFolder(
        id: String,
        name: String,
        path: String,
        ownerId: String,
    ): StorageItem {
        val now = Clock.System.now()
        return StorageItem(
            id = id,
            name = name,
            path = path,
            type = ItemType.FOLDER,
            parentId = null,
            ownerId = ownerId,
            size = 0,
            mimeType = null,
            checksum = null,
            storageKey = null,
            visibility = Visibility.PRIVATE,
            isTrashed = false,
            isStarred = false,
            createdAt = now,
            updatedAt = now,
            trashedAt = null,
            version = 1,
        )
    }
}
