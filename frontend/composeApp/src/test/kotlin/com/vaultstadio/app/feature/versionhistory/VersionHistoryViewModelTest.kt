/**
 * Unit tests for VersionHistoryViewModel: loadHistory, clearSelectedVersion,
 * downloadVersion, clearDownloadUrl, clearDiff, clearError, getVersion, deleteVersion error path.
 */

package com.vaultstadio.app.feature.versionhistory

import com.vaultstadio.app.domain.config.usecase.GetVersionUrlsUseCase
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.version.model.FileVersion
import com.vaultstadio.app.domain.version.model.FileVersionHistory
import com.vaultstadio.app.domain.version.model.VersionDiff
import com.vaultstadio.app.domain.version.usecase.CleanupVersionsUseCase
import com.vaultstadio.app.domain.version.usecase.CompareVersionsUseCase
import com.vaultstadio.app.domain.version.usecase.DeleteVersionUseCase
import com.vaultstadio.app.domain.version.usecase.GetVersionHistoryUseCase
import com.vaultstadio.app.domain.version.usecase.GetVersionUseCase
import com.vaultstadio.app.domain.version.usecase.RestoreVersionUseCase
import com.vaultstadio.app.feature.ViewModelTestBase
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testFileVersion(
    id: String = "ver-1",
    itemId: String = "item-1",
    versionNumber: Int = 1,
) = FileVersion(
    id = id,
    itemId = itemId,
    versionNumber = versionNumber,
    size = 1024L,
    checksum = "abc",
    createdBy = "user-1",
    createdAt = testInstant,
    comment = null,
    isLatest = true,
    restoredFrom = null,
)

private fun testFileVersionHistory(itemId: String = "item-1") = FileVersionHistory(
    itemId = itemId,
    itemName = "file.txt",
    versions = listOf(testFileVersion(itemId = itemId)),
    totalVersions = 1,
    totalSize = 1024L,
)

private fun testVersionDiff() = VersionDiff(
    fromVersion = 1,
    toVersion = 2,
    sizeChange = 100L,
    additions = 5,
    deletions = 2,
    isBinary = false,
)

private class FakeGetVersionHistoryUseCase(
    var result: Result<FileVersionHistory> = Result.success(testFileVersionHistory()),
) : GetVersionHistoryUseCase {
    override suspend fun invoke(itemId: String): Result<FileVersionHistory> = result
}

private class FakeGetVersionUseCase(
    var result: Result<FileVersion> = Result.success(testFileVersion()),
) : GetVersionUseCase {
    override suspend fun invoke(itemId: String, versionNumber: Int): Result<FileVersion> = result
}

private class FakeRestoreVersionUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : RestoreVersionUseCase {
    override suspend fun invoke(itemId: String, versionNumber: Int, comment: String?): Result<Unit> = result
}

private class FakeCompareVersionsUseCase(
    var result: Result<VersionDiff> = Result.success(testVersionDiff()),
) : CompareVersionsUseCase {
    override suspend fun invoke(itemId: String, fromVersion: Int, toVersion: Int): Result<VersionDiff> = result
}

private class FakeDeleteVersionUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : DeleteVersionUseCase {
    override suspend fun invoke(versionId: String): Result<Unit> = result
}

private class FakeCleanupVersionsUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : CleanupVersionsUseCase {
    override suspend fun invoke(
        itemId: String,
        maxVersions: Int?,
        maxAgeDays: Int?,
        minVersionsToKeep: Int,
    ): Result<Unit> = result
}

private class FakeGetVersionUrlsUseCase(
    var downloadUrlResult: String = "https://api.test/version/download",
) : GetVersionUrlsUseCase {
    override fun downloadUrl(itemId: String, versionNumber: Int): String = downloadUrlResult
}

class VersionHistoryViewModelTest {

    private val itemId = "item-1"

    private fun createViewModel(
        getHistoryResult: Result<FileVersionHistory> = Result.success(testFileVersionHistory()),
    ): VersionHistoryViewModel = VersionHistoryViewModel(
        getVersionHistoryUseCase = FakeGetVersionHistoryUseCase(getHistoryResult),
        getVersionUseCase = FakeGetVersionUseCase(),
        restoreVersionUseCase = FakeRestoreVersionUseCase(),
        compareVersionsUseCase = FakeCompareVersionsUseCase(),
        deleteVersionUseCase = FakeDeleteVersionUseCase(),
        cleanupVersionsUseCase = FakeCleanupVersionsUseCase(),
        getVersionUrlsUseCase = FakeGetVersionUrlsUseCase(),
        initialItemId = itemId,
    )

    @Test
    fun loadHistory_success_populatesVersionHistory() = ViewModelTestBase.runTestWithMain {
        val history = testFileVersionHistory()
        val vm = createViewModel(getHistoryResult = Result.success(history))
        vm.loadHistory(itemId)
        assertEquals(history, vm.versionHistory)
        assertNull(vm.error)
    }

    @Test
    fun loadHistory_error_setsError() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getHistoryResult = Result.error("ERR", "Load failed"))
        vm.loadHistory(itemId)
        assertNull(vm.versionHistory)
        assertEquals("Load failed", vm.error)
    }

    @Test
    fun getVersion_success_setsSelectedVersion() = ViewModelTestBase.runTestWithMain {
        val version = testFileVersion()
        val vm = createViewModel()
        vm.getVersion(itemId, 1)
        assertEquals(version, vm.selectedVersion)
    }

    @Test
    fun clearSelectedVersion_clearsSelection() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel()
        vm.getVersion(itemId, 1)
        assertTrue(vm.selectedVersion != null)
        vm.clearSelectedVersion()
        assertNull(vm.selectedVersion)
    }

    @Test
    fun downloadVersion_setsDownloadUrl() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel()
        vm.downloadVersion("f1", 2)
        assertEquals("https://api.test/version/download", vm.downloadUrl)
    }

    @Test
    fun clearDownloadUrl_clearsDownloadUrl() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel()
        vm.downloadVersion(itemId, 1)
        vm.clearDownloadUrl()
        assertNull(vm.downloadUrl)
    }

    @Test
    fun compareVersions_success_setsVersionDiff() = ViewModelTestBase.runTestWithMain {
        val diff = testVersionDiff()
        val vm = createViewModel()
        vm.compareVersions(itemId, 1, 2)
        assertEquals(diff, vm.versionDiff)
    }

    @Test
    fun clearDiff_clearsVersionDiff() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel()
        vm.compareVersions(itemId, 1, 2)
        assertTrue(vm.versionDiff != null)
        vm.clearDiff()
        assertNull(vm.versionDiff)
    }

    @Test
    fun clearError_clearsErrorMessage() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getHistoryResult = Result.error("ERR", "Oops"))
        vm.loadHistory(itemId)
        assertEquals("Oops", vm.error)
        vm.clearError()
        assertNull(vm.error)
    }

    @Test
    fun deleteVersion_error_setsError() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel()
        vm.loadHistory(itemId)
        val deleteUseCase = FakeDeleteVersionUseCase(Result.error("ERR", "Delete failed"))
        val viewModel = VersionHistoryViewModel(
            getVersionHistoryUseCase = FakeGetVersionHistoryUseCase(Result.success(testFileVersionHistory())),
            getVersionUseCase = FakeGetVersionUseCase(),
            restoreVersionUseCase = FakeRestoreVersionUseCase(),
            compareVersionsUseCase = FakeCompareVersionsUseCase(),
            deleteVersionUseCase = deleteUseCase,
            cleanupVersionsUseCase = FakeCleanupVersionsUseCase(),
            getVersionUrlsUseCase = FakeGetVersionUrlsUseCase(),
            initialItemId = itemId,
        )
        viewModel.loadHistory(itemId)
        viewModel.deleteVersion("ver-1")
        assertEquals("Delete failed", viewModel.error)
    }
}
