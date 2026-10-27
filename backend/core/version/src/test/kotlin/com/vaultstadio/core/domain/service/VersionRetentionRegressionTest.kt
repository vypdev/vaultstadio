package com.vaultstadio.core.domain.service

import arrow.core.right
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.model.VersionRetentionPolicy
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionRetentionRegressionTest : VersionRegressionFixture() {
    @Test
    fun `applyRetentionPolicy should return only scoped deleted version ids`() = runTest {
        val itemId = "item-1"
        val now = Clock.System.now()
        val oldVersion = FileVersion(
            id = "v-old",
            itemId = itemId,
            versionNumber = 1,
            size = 1024,
            checksum = "c1",
            storageKey = "storage/old/key",
            createdBy = "user-1",
            createdAt = now,
            isLatest = false,
        )
        coEvery { versionRepository.applyRetentionPolicy(itemId, VersionRetentionPolicy.DEFAULT) } returns
            listOf("v-old").right()
        coEvery { versionRepository.listVersions(itemId, Int.MAX_VALUE, 0) } returns listOf(oldVersion).right()
        coEvery { storageBackend.delete(any()) } returns Unit.right()

        val result = service.applyRetentionPolicy(itemId, "user-1", VersionRetentionPolicy.DEFAULT)

        assertTrue(result.isRight())
        result.onRight { deletedIds ->
            assertEquals(1, deletedIds.size)
            assertEquals("v-old", deletedIds[0])
        }
    }

    @Test
    fun `cleanupVersionsForItem should delete all version files and remove from repository`() = runTest {
        val itemId = "item-1"
        val now = Clock.System.now()
        val versions = listOf(
            FileVersion(
                id = "v1",
                itemId = itemId,
                versionNumber = 1,
                size = 100,
                checksum = "c1",
                storageKey = "key1",
                createdBy = "user-1",
                createdAt = now,
                isLatest = false,
            ),
        )
        coEvery { versionRepository.listVersions(itemId, Int.MAX_VALUE, 0) } returns versions.right()
        coEvery { storageBackend.delete(any()) } returns Unit.right()
        coEvery { versionRepository.deleteAllForItem(itemId) } returns 1.right()

        val result = service.cleanupVersionsForItem(itemId, "user-1")

        assertTrue(result.isRight())
        result.onRight { count -> assertEquals(1, count) }
    }
}
