/**
 * Unit tests for version use cases (GetVersionHistory, GetVersion, RestoreVersion, etc.).
 * Uses a fake VersionRepository to avoid platform/DI.
 */

package com.vaultstadio.app.domain.usecase.version

import com.vaultstadio.app.data.version.usecase.CleanupVersionsUseCaseImpl
import com.vaultstadio.app.data.version.usecase.CompareVersionsUseCaseImpl
import com.vaultstadio.app.data.version.usecase.DeleteVersionUseCaseImpl
import com.vaultstadio.app.data.version.usecase.GetVersionHistoryUseCaseImpl
import com.vaultstadio.app.data.version.usecase.GetVersionUseCaseImpl
import com.vaultstadio.app.data.version.usecase.RestoreVersionUseCaseImpl
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.version.VersionRepository
import com.vaultstadio.app.domain.version.model.FileVersion
import com.vaultstadio.app.domain.version.model.FileVersionHistory
import com.vaultstadio.app.domain.version.model.VersionDiff
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testFileVersion(
    id: String = "v1",
    itemId: String = "item-1",
    versionNumber: Int = 1,
) = FileVersion(
    id = id,
    itemId = itemId,
    versionNumber = versionNumber,
    size = 100L,
    checksum = "abc",
    createdBy = "user",
    createdAt = testInstant,
    comment = null,
    isLatest = true,
    restoredFrom = null,
)

private fun testFileVersionHistory(
    itemId: String = "item-1",
    itemName: String = "file.txt",
    versions: List<FileVersion> = listOf(testFileVersion()),
) = FileVersionHistory(
    itemId = itemId,
    itemName = itemName,
    versions = versions,
    totalVersions = versions.size,
    totalSize = versions.sumOf { it.size },
)

private class FakeVersionRepository(
    var getVersionHistoryResult: Result<FileVersionHistory> = Result.success(testFileVersionHistory()),
    var getVersionResult: Result<FileVersion> = Result.success(testFileVersion()),
    var restoreVersionResult: Result<Unit> = Result.success(Unit),
    var compareVersionsResult: Result<VersionDiff> = Result.success(VersionDiff(1, 2, 0L, 0, 0, false)),
    var deleteVersionResult: Result<Unit> = Result.success(Unit),
    var cleanupVersionsResult: Result<Unit> = Result.success(Unit),
) : VersionRepository {

    override suspend fun getVersionHistory(itemId: String): Result<FileVersionHistory> = getVersionHistoryResult

    override suspend fun getVersion(itemId: String, versionNumber: Int): Result<FileVersion> = getVersionResult

    override suspend fun restoreVersion(itemId: String, versionNumber: Int, comment: String?): Result<Unit> =
        restoreVersionResult

    override suspend fun compareVersions(itemId: String, fromVersion: Int, toVersion: Int): Result<VersionDiff> =
        compareVersionsResult

    override suspend fun deleteVersion(versionId: String): Result<Unit> = deleteVersionResult

    override suspend fun cleanupVersions(
        itemId: String,
        maxVersions: Int?,
        maxAgeDays: Int?,
        minVersionsToKeep: Int,
    ): Result<Unit> = cleanupVersionsResult

    override fun getVersionDownloadUrl(itemId: String, versionNumber: Int): String =
        "https://api.test/versions/$itemId/$versionNumber/download"
}

class GetVersionHistoryUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetVersionHistoryResult() = runTest {
        val history = testFileVersionHistory(itemId = "f1", itemName = "doc.pdf")
        val repo = FakeVersionRepository(getVersionHistoryResult = Result.success(history))
        val useCase = GetVersionHistoryUseCaseImpl(repo)
        val result = useCase("f1")
        assertTrue(result.isSuccess())
        assertEquals(history, result.getOrNull())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeVersionRepository(getVersionHistoryResult = Result.error("NOT_FOUND", "Item not found"))
        val useCase = GetVersionHistoryUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class GetVersionUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetVersionResult() = runTest {
        val version = testFileVersion(versionNumber = 2)
        val repo = FakeVersionRepository(getVersionResult = Result.success(version))
        val useCase = GetVersionUseCaseImpl(repo)
        val result = useCase("item-1", 2)
        assertTrue(result.isSuccess())
        assertEquals(version, result.getOrNull())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeVersionRepository(getVersionResult = Result.error("NOT_FOUND", "Version not found"))
        val useCase = GetVersionUseCaseImpl(repo)
        val result = useCase("item-1", 99)
        assertTrue(result.isError())
    }
}

class RestoreVersionUseCaseTest {

    @Test
    fun invoke_returnsRepositoryRestoreVersionResult() = runTest {
        val repo = FakeVersionRepository(restoreVersionResult = Result.success(Unit))
        val useCase = RestoreVersionUseCaseImpl(repo)
        val result = useCase("item-1", 1, "Restored for comparison")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_withNullComment_forwardsToRepository() = runTest {
        val repo = FakeVersionRepository(restoreVersionResult = Result.success(Unit))
        val useCase = RestoreVersionUseCaseImpl(repo)
        val result = useCase("item-1", 1, null)
        assertTrue(result.isSuccess())
    }
}

class CompareVersionsUseCaseTest {

    @Test
    fun invoke_returnsRepositoryCompareVersionsResult() = runTest {
        val diff = VersionDiff(1, 2, 100L, 5, 3, false)
        val repo = FakeVersionRepository(compareVersionsResult = Result.success(diff))
        val useCase = CompareVersionsUseCaseImpl(repo)
        val result = useCase("item-1", 1, 2)
        assertTrue(result.isSuccess())
        assertEquals(diff, result.getOrNull())
    }
}

class DeleteVersionUseCaseTest {

    @Test
    fun invoke_returnsRepositoryDeleteVersionResult() = runTest {
        val repo = FakeVersionRepository(deleteVersionResult = Result.success(Unit))
        val useCase = DeleteVersionUseCaseImpl(repo)
        val result = useCase("version-id-1")
        assertTrue(result.isSuccess())
    }
}

class CleanupVersionsUseCaseTest {

    @Test
    fun invoke_returnsRepositoryCleanupVersionsResult() = runTest {
        val repo = FakeVersionRepository(cleanupVersionsResult = Result.success(Unit))
        val useCase = CleanupVersionsUseCaseImpl(repo)
        val result = useCase("item-1", maxVersions = 10, maxAgeDays = 30, minVersionsToKeep = 1)
        assertTrue(result.isSuccess())
    }
}
