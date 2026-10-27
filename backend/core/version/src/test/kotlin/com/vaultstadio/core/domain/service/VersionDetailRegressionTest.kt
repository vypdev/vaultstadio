package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionDetailRegressionTest : VersionRegressionFixture() {
    @Test
    fun `getVersion should return version when found`() = runTest {
        val now = Clock.System.now()
        val version = FileVersion(
            id = "v1",
            itemId = "item-1",
            versionNumber = 1,
            size = 1024,
            checksum = "c1",
            storageKey = "key1",
            createdBy = "user-1",
            createdAt = now,
            isLatest = false,
        )
        coEvery { versionRepository.findByItemAndVersion("item-1", 1) } returns version.right()

        val result = service.getVersion("item-1", 1, "user-1")

        assertTrue(result.isRight())
        result.onRight { v ->
            assertEquals(1, v.versionNumber)
            assertEquals(1024, v.size)
        }
    }

    @Test
    fun `getVersion should fail when version not found`() = runTest {
        coEvery { versionRepository.findByItemAndVersion("item-1", 99) } returns null.right()

        val result = service.getVersion("item-1", 99, "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }

    @Test
    fun `getVersion should propagate when versionRepository findByItemAndVersion returns Left`() = runTest {
        coEvery { versionRepository.findByItemAndVersion("item-1", 1) } returns DatabaseException("db error").left()

        val result = service.getVersion("item-1", 1, "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is DatabaseException)
    }

    @Test
    fun `getLatestVersion should fail when no versions exist`() = runTest {
        coEvery { versionRepository.findLatest("item-1") } returns null.right()

        val result = service.getLatestVersion("item-1", "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }

    @Test
    fun `getLatestVersion should propagate when versionRepository findLatest returns Left`() = runTest {
        coEvery { versionRepository.findLatest("item-1") } returns DatabaseException("db error").left()

        val result = service.getLatestVersion("item-1", "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is DatabaseException)
    }

    @Test
    fun `getLatestVersion should return version when found`() = runTest {
        val now = Clock.System.now()
        val latest = FileVersion(
            id = "v2",
            itemId = "item-1",
            versionNumber = 2,
            size = 2048,
            checksum = "c2",
            storageKey = "key2",
            createdBy = "user-1",
            createdAt = now,
            isLatest = true,
        )
        coEvery { versionRepository.findLatest("item-1") } returns latest.right()

        val result = service.getLatestVersion("item-1", "user-1")

        assertTrue(result.isRight())
        result.onRight { v ->
            assertEquals(2, v.versionNumber)
            assertEquals(2048, v.size)
            assertTrue(v.isLatest)
        }
    }
}
