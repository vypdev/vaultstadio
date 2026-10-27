package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.model.FileVersionHistory
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionHistoryRegressionTest : VersionRegressionFixture() {
    @Test
    fun `getHistory should return version history`() = runTest {
        val itemId = "item-1"
        val now = Clock.System.now()

        val history = FileVersionHistory(
            item = StorageItem(
                id = itemId,
                name = "test.txt",
                path = "/test.txt",
                type = ItemType.FILE,
                ownerId = "user-1",
                createdAt = now,
                updatedAt = now,
            ),
            versions = listOf(
                FileVersion(
                    id = "v2",
                    itemId = itemId,
                    versionNumber = 2,
                    size = 2048,
                    checksum = "xyz789",
                    storageKey = "storage/v2/file",
                    createdBy = "user-1",
                    createdAt = now,
                    isLatest = true,
                ),
                FileVersion(
                    id = "v1",
                    itemId = itemId,
                    versionNumber = 1,
                    size = 1024,
                    checksum = "abc123",
                    storageKey = "storage/v1/file",
                    createdBy = "user-1",
                    createdAt = now,
                    isLatest = false,
                ),
            ),
            totalVersions = 2,
            totalSize = 3072,
        )

        coEvery { versionRepository.getHistory(itemId) } returns history.right()

        val result = service.getHistory(itemId, "user-1")

        assertTrue(result.isRight())
        result.onRight { h ->
            assertEquals(2, h.totalVersions)
            assertEquals(3072, h.totalSize)
            assertEquals(2, h.latestVersion?.versionNumber)
        }
    }

    @Test
    fun `listVersions should return paginated versions`() = runTest {
        val itemId = "item-1"
        val now = Clock.System.now()

        val versions = listOf(
            FileVersion(
                id = "v1",
                itemId = itemId,
                versionNumber = 1,
                size = 1024,
                checksum = "abc",
                storageKey = "key1",
                createdBy = "user-1",
                createdAt = now,
                isLatest = true,
            ),
        )

        coEvery { versionRepository.listVersions(itemId, 10, 0) } returns versions.right()

        val result = service.listVersions(itemId, "user-1", 10, 0)

        assertTrue(result.isRight())
        result.onRight { list ->
            assertEquals(1, list.size)
        }
    }

    @Test
    fun `listVersions should propagate when repository returns Left`() = runTest {
        coEvery { versionRepository.listVersions("item-1", 10, 0) } returns DatabaseException("db error").left()

        val result = service.listVersions("item-1", "user-1", 10, 0)

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is DatabaseException)
    }

    @Test
    fun `getVersionCount should return count`() = runTest {
        coEvery { versionRepository.countVersions("item-1") } returns 5.right()

        val result = service.getVersionCount("item-1", "user-1")

        assertTrue(result.isRight())
        result.onRight { count ->
            assertEquals(5, count)
        }
    }

    @Test
    fun `getTotalVersionSize should return total size`() = runTest {
        coEvery { versionRepository.getTotalVersionSize("item-1") } returns 10240L.right()

        val result = service.getTotalVersionSize("item-1", "user-1")

        assertTrue(result.isRight())
        result.onRight { size ->
            assertEquals(10240L, size)
        }
    }

    @Test
    fun `getVersionCount should propagate when repository returns Left`() = runTest {
        val repoError = DatabaseException("db error")
        coEvery { versionRepository.countVersions("item-1") } returns repoError.left()

        val result = service.getVersionCount("item-1", "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is DatabaseException)
    }

    @Test
    fun `getTotalVersionSize should propagate when repository returns Left`() = runTest {
        val repoError = DatabaseException("db error")
        coEvery { versionRepository.getTotalVersionSize("item-1") } returns repoError.left()

        val result = service.getTotalVersionSize("item-1", "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is DatabaseException)
    }

    @Test
    fun `getHistory should propagate when repository returns Left`() = runTest {
        coEvery { versionRepository.getHistory("item-1") } returns DatabaseException("db error").left()

        val result = service.getHistory("item-1", "user-1")

        assertTrue(result.isLeft())
    }
}
