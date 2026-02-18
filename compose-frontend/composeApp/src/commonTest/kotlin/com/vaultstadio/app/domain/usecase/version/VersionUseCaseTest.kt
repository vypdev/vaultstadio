/**
 * Unit tests for version use cases (GetVersionHistory, GetVersion, RestoreVersion, etc.).
 * Uses a fake VersionRepository to avoid platform/DI.
 */

package com.vaultstadio.app.domain.usecase.version

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.VersionRepository
import com.vaultstadio.app.domain.model.FileVersion
import com.vaultstadio.app.domain.model.FileVersionHistory
import com.vaultstadio.app.domain.model.VersionDiff
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
    var getVersionHistoryResult: ApiResult<FileVersionHistory> = ApiResult.success(testFileVersionHistory()),
    var getVersionResult: ApiResult<FileVersion> = ApiResult.success(testFileVersion()),
    var restoreVersionResult: ApiResult<Unit> = ApiResult.success(Unit),
    var compareVersionsResult: ApiResult<VersionDiff> = ApiResult.success(VersionDiff(1, 2, 0L, 0, 0, false)),
    var deleteVersionResult: ApiResult<Unit> = ApiResult.success(Unit),
    var cleanupVersionsResult: ApiResult<Unit> = ApiResult.success(Unit),
) : VersionRepository {

    override suspend fun getVersionHistory(itemId: String): ApiResult<FileVersionHistory> = getVersionHistoryResult

    override suspend fun getVersion(itemId: String, versionNumber: Int): ApiResult<FileVersion> = getVersionResult

    override suspend fun restoreVersion(itemId: String, versionNumber: Int, comment: String?): ApiResult<Unit> =
        restoreVersionResult

    override suspend fun compareVersions(itemId: String, fromVersion: Int, toVersion: Int): ApiResult<VersionDiff> =
        compareVersionsResult

    override suspend fun deleteVersion(versionId: String): ApiResult<Unit> = deleteVersionResult

    override suspend fun cleanupVersions(
        itemId: String,
        maxVersions: Int?,
        maxAgeDays: Int?,
        minVersionsToKeep: Int,
    ): ApiResult<Unit> = cleanupVersionsResult

    override fun getVersionDownloadUrl(itemId: String, versionNumber: Int): String =
        "https://api.test/versions/$itemId/$versionNumber/download"
}

class GetVersionHistoryUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetVersionHistoryResult() = runTest {
        val history = testFileVersionHistory(itemId = "f1", itemName = "doc.pdf")
        val repo = FakeVersionRepository(getVersionHistoryResult = ApiResult.success(history))
        val useCase = GetVersionHistoryUseCaseImpl(repo)
        val result = useCase("f1")
        assertTrue(result.isSuccess())
        assertEquals(history, result.getOrNull())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeVersionRepository(getVersionHistoryResult = ApiResult.error("NOT_FOUND", "Item not found"))
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
        val repo = FakeVersionRepository(getVersionResult = ApiResult.success(version))
        val useCase = GetVersionUseCaseImpl(repo)
        val result = useCase("item-1", 2)
        assertTrue(result.isSuccess())
        assertEquals(version, result.getOrNull())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeVersionRepository(getVersionResult = ApiResult.error("NOT_FOUND", "Version not found"))
        val useCase = GetVersionUseCaseImpl(repo)
        val result = useCase("item-1", 99)
        assertTrue(result.isError())
    }
}

class RestoreVersionUseCaseTest {

    @Test
    fun invoke_returnsRepositoryRestoreVersionResult() = runTest {
        val repo = FakeVersionRepository(restoreVersionResult = ApiResult.success(Unit))
        val useCase = RestoreVersionUseCaseImpl(repo)
        val result = useCase("item-1", 1, "Restored for comparison")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_withNullComment_forwardsToRepository() = runTest {
        val repo = FakeVersionRepository(restoreVersionResult = ApiResult.success(Unit))
        val useCase = RestoreVersionUseCaseImpl(repo)
        val result = useCase("item-1", 1, null)
        assertTrue(result.isSuccess())
    }
}

class CompareVersionsUseCaseTest {

    @Test
    fun invoke_returnsRepositoryCompareVersionsResult() = runTest {
        val diff = VersionDiff(1, 2, 100L, 5, 3, false)
        val repo = FakeVersionRepository(compareVersionsResult = ApiResult.success(diff))
        val useCase = CompareVersionsUseCaseImpl(repo)
        val result = useCase("item-1", 1, 2)
        assertTrue(result.isSuccess())
        assertEquals(diff, result.getOrNull())
    }
}

class DeleteVersionUseCaseTest {

    @Test
    fun invoke_returnsRepositoryDeleteVersionResult() = runTest {
        val repo = FakeVersionRepository(deleteVersionResult = ApiResult.success(Unit))
        val useCase = DeleteVersionUseCaseImpl(repo)
        val result = useCase("version-id-1")
        assertTrue(result.isSuccess())
    }
}

class CleanupVersionsUseCaseTest {

    @Test
    fun invoke_returnsRepositoryCleanupVersionsResult() = runTest {
        val repo = FakeVersionRepository(cleanupVersionsResult = ApiResult.success(Unit))
        val useCase = CleanupVersionsUseCaseImpl(repo)
        val result = useCase("item-1", maxVersions = 10, maxAgeDays = 30, minVersionsToKeep = 1)
        assertTrue(result.isSuccess())
    }
}
