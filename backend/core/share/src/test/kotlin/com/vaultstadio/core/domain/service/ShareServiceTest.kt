/**
 * VaultStadio Share Service Tests
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.event.ShareEvent
import com.vaultstadio.domain.common.exception.AuthorizationException
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
    }

    @Nested
    inner class AccessShareTests {

        @Test
        fun `should access share successfully`() = runTest {
            // Given
            val share = createTestShare("share-id", testItemId, testUserId)
            val item = createTestItem(testItemId, testUserId)
            val input = AccessShareInput(token = share.token)

            coEvery { shareRepository.findByToken(share.token) } returns share.right()
            coEvery { storageItemRepository.findById(testItemId) } returns item.right()
            coEvery { shareRepository.incrementDownloadCount("share-id") } returns 1.right()

            // When
            val result = shareService.accessShare(input)

            // Then
            assertTrue(result.isRight())
            val (returnedShare, returnedItem) = (result as Either.Right).value
            assertEquals(share.id, returnedShare.id)
            assertEquals(item.id, returnedItem.id)

            coVerify { shareRepository.incrementDownloadCount("share-id") }
        }

        @Test
        fun `should fail when share not found`() = runTest {
            // Given
            val input = AccessShareInput(token = "invalid-token")

            coEvery { shareRepository.findByToken("invalid-token") } returns null.right()

            // When
            val result = shareService.accessShare(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ItemNotFoundException)
        }

        @Test
        fun `should fail when share is inactive`() = runTest {
            // Given
            val share = createTestShare("share-id", testItemId, testUserId).copy(isActive = false)
            val input = AccessShareInput(token = share.token)

            coEvery { shareRepository.findByToken(share.token) } returns share.right()

            // When
            val result = shareService.accessShare(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is AuthorizationException)
            assertTrue((result as Either.Left).value.message?.contains("active") == true)
        }

        @Test
        fun `should fail when share is expired`() = runTest {
            // Given
            val expiredShare = createTestShare("share-id", testItemId, testUserId).copy(
                expiresAt = Clock.System.now() - 1.days,
            )
            val input = AccessShareInput(token = expiredShare.token)

            coEvery { shareRepository.findByToken(expiredShare.token) } returns expiredShare.right()

            // When
            val result = shareService.accessShare(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is AuthorizationException)
            assertTrue((result as Either.Left).value.message?.contains("expired") == true)
        }

        @Test
        fun `should fail when download limit reached`() = runTest {
            // Given
            val maxedShare = createTestShare("share-id", testItemId, testUserId).copy(
                maxDownloads = 5,
                downloadCount = 5,
            )
            val input = AccessShareInput(token = maxedShare.token)

            coEvery { shareRepository.findByToken(maxedShare.token) } returns maxedShare.right()

            // When
            val result = shareService.accessShare(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is AuthorizationException)
            assertTrue((result as Either.Left).value.message?.contains("limit") == true)
        }

        @Test
        fun `should fail when password required but not provided`() = runTest {
            // Given
            val protectedShare = createTestShare("share-id", testItemId, testUserId).copy(
                password = "hashed_password",
            )
            val input = AccessShareInput(token = protectedShare.token, password = null)

            coEvery { shareRepository.findByToken(protectedShare.token) } returns protectedShare.right()

            // When
            val result = shareService.accessShare(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is AuthorizationException)
            assertTrue((result as Either.Left).value.message?.contains("Password") == true)
        }

        @Test
        fun `should fail when password is incorrect`() = runTest {
            // Given
            val protectedShare = createTestShare("share-id", testItemId, testUserId).copy(
                password = "hashed_password",
            )
            val input = AccessShareInput(token = protectedShare.token, password = "wrong")

            coEvery { shareRepository.findByToken(protectedShare.token) } returns protectedShare.right()
            every { passwordHasher.verify("wrong", "hashed_password") } returns false

            // When
            val result = shareService.accessShare(input)

            // Then
            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is AuthorizationException)
            assertTrue((result as Either.Left).value.message?.contains("Invalid") == true)
        }

        @Test
        fun `should succeed with correct password`() = runTest {
            // Given
            val protectedShare = createTestShare("share-id", testItemId, testUserId).copy(
                password = "hashed_password",
            )
            val item = createTestItem(testItemId, testUserId)
            val input = AccessShareInput(token = protectedShare.token, password = "correct")

            coEvery { shareRepository.findByToken(protectedShare.token) } returns protectedShare.right()
            every { passwordHasher.verify("correct", "hashed_password") } returns true
            coEvery { storageItemRepository.findById(testItemId) } returns item.right()
            coEvery { shareRepository.incrementDownloadCount("share-id") } returns 1.right()

            // When
            val result = shareService.accessShare(input)

            // Then
            assertTrue(result.isRight())
        }

        @Test
        fun `should fail when shared item not found`() = runTest {
            val share = createTestShare("share-id", "missing-item", testUserId)
            val input = AccessShareInput(token = share.token)

            coEvery { shareRepository.findByToken(share.token) } returns share.right()
            coEvery { storageItemRepository.findById("missing-item") } returns null.right()

            val result = shareService.accessShare(input)

            assertTrue(result.isLeft())
            assertTrue((result as Either.Left).value is ItemNotFoundException)
            assertTrue((result as Either.Left).value.message?.contains("Shared item not found") == true)
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
