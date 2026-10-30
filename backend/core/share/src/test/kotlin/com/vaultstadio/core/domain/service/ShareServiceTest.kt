/**
 * VaultStadio Share Service Tests
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.event.ShareEvent
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.domain.share.repository.ShareRepository
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.model.Visibility
import com.vaultstadio.domain.storage.repository.StorageItemRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class ShareServiceTest {

    private lateinit var shareRepository: ShareRepository
    private lateinit var storageItemRepository: StorageItemRepository
    private lateinit var passwordHasher: PasswordHasher
    private lateinit var eventBus: EventBus
    private lateinit var shareService: ShareService

    private val testUserId = "user-123"
    private val testItemId = "item-456"

    @BeforeEach
    fun setup() {
        shareRepository = mockk()
        storageItemRepository = mockk()
        passwordHasher = mockk()
        eventBus = mockk(relaxed = true)
        shareService = ShareService(shareRepository, storageItemRepository, passwordHasher, eventBus)
    }

    @Nested
    inner class CreateShareTests {

        @Test
        fun `should create share successfully`() = runTest {
            // Given
            val item = createTestItem(testItemId, testUserId)
            val input = CreateShareInput(
                itemId = testItemId,
                userId = testUserId,
                expirationDays = 7,
            )

            coEvery { storageItemRepository.findById(testItemId) } returns item.right()
            coEvery { shareRepository.create(any()) } answers { firstArg<ShareLink>().right() }

            // When
            val result = shareService.createShare(input)

            // Then
            assertTrue(result.isRight())
            val share = (result as Either.Right).value
            assertEquals(testItemId, share.itemId)
            assertEquals(testUserId, share.createdBy)
            assertTrue(share.isActive)
            assertNotNull(share.token)
            assertNotNull(share.expiresAt)

            coVerify { eventBus.publish(any<ShareEvent.Created>()) }
        }

        @Test
        fun `should create share with password`() = runTest {
            // Given
            val item = createTestItem(testItemId, testUserId)
            val input = CreateShareInput(
                itemId = testItemId,
                userId = testUserId,
                password = "secret123",
            )

            coEvery { storageItemRepository.findById(testItemId) } returns item.right()
            every { passwordHasher.hash("secret123") } returns "hashed_password"
            coEvery { shareRepository.create(any()) } answers { firstArg<ShareLink>().right() }

            // When
            val result = shareService.createShare(input)

            // Then
            assertTrue(result.isRight())
            val share = (result as Either.Right).value
            assertEquals("hashed_password", share.password)
        }

        @Test
        fun `should create share with download limit`() = runTest {
            // Given
            val item = createTestItem(testItemId, testUserId)
            val input = CreateShareInput(
                itemId = testItemId,
                userId = testUserId,
                maxDownloads = 5,
            )

            coEvery { storageItemRepository.findById(testItemId) } returns item.right()
            coEvery { shareRepository.create(any()) } answers { firstArg<ShareLink>().right() }

            // When
            val result = shareService.createShare(input)

            // Then
            assertTrue(result.isRight())
            val share = (result as Either.Right).value
            assertEquals(5, share.maxDownloads)
            assertEquals(0, share.downloadCount)
        }

        @Test
        fun `should fail when item not found`() = runTest {
            // Given
            val input = CreateShareInput(
                itemId = "missing-item",
                userId = testUserId,
            )

            coEvery { storageItemRepository.findById("missing-item") } returns null.right()

            // When
            val result = shareService.createShare(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ItemNotFoundException)
        }

        @Test
        fun `should propagate when storageItemRepository findById returns Left`() = runTest {
            val input = CreateShareInput(
                itemId = testItemId,
                userId = testUserId,
            )
            val repoError = DatabaseException("database unavailable")
            coEvery { storageItemRepository.findById(testItemId) } returns repoError.left()

            val result = shareService.createShare(input)

            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is DatabaseException)
        }

        @Test
        fun `should fail when user is not owner`() = runTest {
            // Given
            val item = createTestItem(testItemId, "other-user")
            val input = CreateShareInput(
                itemId = testItemId,
                userId = testUserId,
            )

            coEvery { storageItemRepository.findById(testItemId) } returns item.right()

            // When
            val result = shareService.createShare(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is AuthorizationException)
        }

        @Test
        fun `should propagate when shareRepository create returns Left`() = runTest {
            val item = createTestItem(testItemId, testUserId)
            val input = CreateShareInput(itemId = testItemId, userId = testUserId)
            coEvery { storageItemRepository.findById(testItemId) } returns item.right()
            val repoError = DatabaseException("create failed")
            coEvery { shareRepository.create(any()) } returns repoError.left()

            val result = shareService.createShare(input)

            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is DatabaseException)
        }
    }

    @Nested
    inner class GetSharesTests {

        @Test
        fun `should get shares by user`() = runTest {
            // Given
            val shares = listOf(
                createTestShare("share-1", "item-1", testUserId),
                createTestShare("share-2", "item-2", testUserId),
            )

            coEvery { shareRepository.findByCreatedBy(testUserId, true) } returns shares.right()

            // When
            val result = shareService.getSharesByUser(testUserId)

            // Then
            assertTrue(result.isRight())
            assertEquals(2, (result as Either.Right).value.size)
        }

        @Test
        fun `should propagate when shareRepository findByCreatedBy returns Left for getSharesByUser`() = runTest {
            val repoError = DatabaseException("database unavailable")
            coEvery { shareRepository.findByCreatedBy(testUserId, true) } returns repoError.left()

            val result = shareService.getSharesByUser(testUserId)

            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is DatabaseException)
        }

        @Test
        fun `should get shares shared with user`() = runTest {
            val shares = listOf(createTestShare("share-1", "item-1", "other-user"))
            coEvery { shareRepository.findSharedWithUser(testUserId, true) } returns shares.right()

            val result = shareService.getSharesSharedWithUser(testUserId)

            assertTrue(result.isRight())
            assertEquals(1, (result as Either.Right).value.size)
        }

        @Test
        fun `should propagate when shareRepository findSharedWithUser returns Left`() = runTest {
            val repoError = DatabaseException("database unavailable")
            coEvery { shareRepository.findSharedWithUser(testUserId, true) } returns repoError.left()

            val result = shareService.getSharesSharedWithUser(testUserId)

            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is DatabaseException)
        }

        @Test
        fun `should get shares by item`() = runTest {
            // Given
            val item = createTestItem(testItemId, testUserId)
            val shares = listOf(
                createTestShare("share-1", testItemId, testUserId),
            )

            coEvery { storageItemRepository.findById(testItemId) } returns item.right()
            coEvery { shareRepository.findByItemId(testItemId) } returns shares.right()

            // When
            val result = shareService.getSharesByItem(testItemId, testUserId)

            // Then
            assertTrue(result.isRight())
            assertEquals(1, (result as Either.Right).value.size)
        }

        @Test
        fun `should fail get shares by item when not owner`() = runTest {
            // Given
            val item = createTestItem(testItemId, "other-user")

            coEvery { storageItemRepository.findById(testItemId) } returns item.right()

            // When
            val result = shareService.getSharesByItem(testItemId, testUserId)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is AuthorizationException)
        }

        @Test
        fun `should fail get shares by item when item not found`() = runTest {
            coEvery { storageItemRepository.findById("missing-item") } returns null.right()

            val result = shareService.getSharesByItem("missing-item", testUserId)

            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ItemNotFoundException)
        }

        @Test
        fun `should propagate when storageItemRepository findById returns Left for getSharesByItem`() = runTest {
            val repoError = DatabaseException("database unavailable")
            coEvery { storageItemRepository.findById(testItemId) } returns repoError.left()

            val result = shareService.getSharesByItem(testItemId, testUserId)

            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is DatabaseException)
        }
    }

    @Nested
    inner class GetShareTests {

        @Test
        fun `should get share by id when found`() = runTest {
            val share = createTestShare("share-1", testItemId, testUserId)
            coEvery { shareRepository.findById("share-1") } returns share.right()

            val result = shareService.getShare("share-1")

            assertTrue(result.isRight())
            assertEquals("share-1", (result as Either.Right).value.id)
        }

        @Test
        fun `should fail get share when not found`() = runTest {
            coEvery { shareRepository.findById("missing-share") } returns null.right()

            val result = shareService.getShare("missing-share")

            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ItemNotFoundException)
        }

        @Test
        fun `should propagate when shareRepository findById returns Left`() = runTest {
            val repoError = DatabaseException("database unavailable")
            coEvery { shareRepository.findById("share-1") } returns repoError.left()

            val result = shareService.getShare("share-1")

            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is DatabaseException)
        }
    }

    @Nested
    inner class DeleteShareTests {

        @Test
        fun `should delete share successfully`() = runTest {
            // Given
            val share = createTestShare("share-id", testItemId, testUserId)

            coEvery { shareRepository.findById("share-id") } returns share.right()
            coEvery { shareRepository.delete("share-id") } returns Unit.right()

            // When
            val result = shareService.deleteShare("share-id", testUserId)

            // Then
            assertTrue(result.isRight())
            coVerify { shareRepository.delete("share-id") }
            coVerify { eventBus.publish(any<ShareEvent.Deleted>()) }
        }

        @Test
        fun `should fail when not creator`() = runTest {
            // Given
            val share = createTestShare("share-id", testItemId, "other-user")

            coEvery { shareRepository.findById("share-id") } returns share.right()

            // When
            val result = shareService.deleteShare("share-id", testUserId)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is AuthorizationException)
        }

        @Test
        fun `should fail when share not found`() = runTest {
            coEvery { shareRepository.findById("missing-share") } returns null.right()

            val result = shareService.deleteShare("missing-share", testUserId)

            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ItemNotFoundException)
        }

        @Test
        fun `should propagate when shareRepository delete returns Left`() = runTest {
            val share = createTestShare("share-id", testItemId, testUserId)
            coEvery { shareRepository.findById("share-id") } returns share.right()
            val repoError = DatabaseException("delete failed")
            coEvery { shareRepository.delete("share-id") } returns repoError.left()

            val result = shareService.deleteShare("share-id", testUserId)

            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is DatabaseException)
        }
    }

    @Nested
    inner class DeactivateShareTests {

        @Test
        fun `should deactivate share successfully`() = runTest {
            // Given
            val share = createTestShare("share-id", testItemId, testUserId)

            coEvery { shareRepository.findById("share-id") } returns share.right()
            coEvery { shareRepository.update(any()) } answers { firstArg<ShareLink>().right() }

            // When
            val result = shareService.deactivateShare("share-id", testUserId)

            // Then
            assertTrue(result.isRight())
            val deactivated = (result as Either.Right).value
            assertEquals(false, deactivated.isActive)
        }

        @Test
        fun `should fail when not creator`() = runTest {
            // Given
            val share = createTestShare("share-id", testItemId, "other-user")

            coEvery { shareRepository.findById("share-id") } returns share.right()

            // When
            val result = shareService.deactivateShare("share-id", testUserId)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is AuthorizationException)
        }

        @Test
        fun `should fail when share not found`() = runTest {
            coEvery { shareRepository.findById("missing-share") } returns null.right()

            val result = shareService.deactivateShare("missing-share", testUserId)

            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ItemNotFoundException)
        }

        @Test
        fun `should propagate when shareRepository update returns Left`() = runTest {
            val share = createTestShare("share-id", testItemId, testUserId)
            coEvery { shareRepository.findById("share-id") } returns share.right()
            val repoError = DatabaseException("update failed")
            coEvery { shareRepository.update(any()) } returns repoError.left()

            val result = shareService.deactivateShare("share-id", testUserId)

            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is DatabaseException)
        }
    }

    // Helper functions

    private fun createTestItem(id: String, ownerId: String): StorageItem {
        val now = Clock.System.now()
        return StorageItem(
            id = id,
            name = "test-file.txt",
            path = "/test-file.txt",
            type = ItemType.FILE,
            parentId = null,
            ownerId = ownerId,
            size = 1024,
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

    private fun createTestShare(id: String, itemId: String, createdBy: String): ShareLink {
        val now = Clock.System.now()
        return ShareLink(
            id = id,
            itemId = itemId,
            token = "token-$id",
            createdBy = createdBy,
            expiresAt = now + 7.days,
            password = null,
            maxDownloads = null,
            downloadCount = 0,
            isActive = true,
            createdAt = now,
        )
    }
}
