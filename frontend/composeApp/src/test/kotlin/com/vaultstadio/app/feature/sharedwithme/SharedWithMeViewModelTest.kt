/**
 * Unit tests for SharedWithMeViewModel: loadSharedItems, onItemClick, clearSelectedItem,
 * downloadItem, clearDownloadUrl, removeShare, clearError.
 */

package com.vaultstadio.app.feature.sharedwithme

import com.vaultstadio.app.domain.config.usecase.GetStorageUrlsUseCase
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.share.model.ShareLink
import com.vaultstadio.app.domain.share.usecase.DeleteShareUseCase
import com.vaultstadio.app.domain.share.usecase.GetSharedWithMeUseCase
import com.vaultstadio.app.domain.storage.model.ItemType
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.domain.storage.model.Visibility
import com.vaultstadio.app.domain.storage.usecase.GetItemUseCase
import com.vaultstadio.app.feature.ViewModelTestBase
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testStorageItem(id: String = "item-1") = StorageItem(
    id = id,
    name = "file.txt",
    path = "/file.txt",
    type = ItemType.FILE,
    parentId = null,
    size = 1024L,
    mimeType = "text/plain",
    visibility = Visibility.PRIVATE,
    isStarred = false,
    isTrashed = false,
    createdAt = testInstant,
    updatedAt = testInstant,
)

private fun testShareLink(itemId: String = "item-1", createdBy: String = "user-1") = ShareLink(
    id = "share-1",
    itemId = itemId,
    token = "tok",
    url = "https://example.com/share/tok",
    expiresAt = testInstant,
    hasPassword = false,
    maxDownloads = null,
    downloadCount = 0,
    isActive = true,
    createdAt = testInstant,
    createdBy = createdBy,
    sharedWithUsers = emptyList(),
)

private class FakeGetSharedWithMeUseCase(
    var result: Result<List<ShareLink>> = Result.success(emptyList()),
) : GetSharedWithMeUseCase {
    override suspend fun invoke(): Result<List<ShareLink>> = result
}

private class FakeGetItemUseCase(
    var result: Result<StorageItem> = Result.success(testStorageItem()),
) : GetItemUseCase {
    override suspend fun invoke(itemId: String): Result<StorageItem> = result
}

private class FakeDeleteShareUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : DeleteShareUseCase {
    override suspend fun invoke(shareId: String): Result<Unit> = result
}

private class FakeGetStorageUrlsUseCase(
    var downloadUrlResult: String = "https://api.test/download/item-1",
) : GetStorageUrlsUseCase {
    override fun downloadUrl(itemId: String): String = downloadUrlResult
    override fun thumbnailUrl(itemId: String, size: String): String = "https://api.test/thumb/$itemId"
    override fun previewUrl(itemId: String): String = "https://api.test/preview/$itemId"
    override fun batchDownloadZipUrl(): String = "https://api.test/batch.zip"
}

class SharedWithMeViewModelTest {

    private fun createViewModel(
        getSharedWithMeResult: Result<List<ShareLink>> = Result.success(emptyList()),
        getItemResult: Result<StorageItem> = Result.success(testStorageItem()),
        deleteShareResult: Result<Unit> = Result.success(Unit),
        downloadUrl: String = "https://api.test/download/item-1",
    ): SharedWithMeViewModel = SharedWithMeViewModel(
        getSharedWithMeUseCase = FakeGetSharedWithMeUseCase(getSharedWithMeResult),
        getItemUseCase = FakeGetItemUseCase(getItemResult),
        deleteShareUseCase = FakeDeleteShareUseCase(deleteShareResult),
        getStorageUrlsUseCase = FakeGetStorageUrlsUseCase(downloadUrl),
    )

    @Test
    fun loadSharedItems_success_emptyList_populatesEmpty() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getSharedWithMeResult = Result.success(emptyList()))
        vm.loadSharedItems()
        assertTrue(vm.sharedItems.isEmpty())
        assertNull(vm.error)
    }

    @Test
    fun loadSharedItems_success_withShareAndItem_populatesSharedItems() = ViewModelTestBase.runTestWithMain {
        val share = testShareLink(itemId = "item-1")
        val item = testStorageItem("item-1")
        val getShared = FakeGetSharedWithMeUseCase(Result.success(listOf(share)))
        val getItem = FakeGetItemUseCase(Result.success(item))
        val vm = SharedWithMeViewModel(
            getSharedWithMeUseCase = getShared,
            getItemUseCase = getItem,
            deleteShareUseCase = FakeDeleteShareUseCase(),
            getStorageUrlsUseCase = FakeGetStorageUrlsUseCase(),
        )
        vm.loadSharedItems()
        assertEquals(1, vm.sharedItems.size)
        assertEquals(item, vm.sharedItems[0].item)
        assertEquals("user-1", vm.sharedItems[0].sharedBy)
        assertNull(vm.error)
    }

    @Test
    fun loadSharedItems_error_setsError() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getSharedWithMeResult = Result.error("ERR", "Load failed"))
        vm.loadSharedItems()
        assertTrue(vm.sharedItems.isEmpty())
        assertEquals("Load failed", vm.error)
    }

    @Test
    fun onItemClick_setsSelectedItem() = ViewModelTestBase.runTestWithMain {
        val item = testStorageItem()
        val vm = createViewModel()
        vm.onItemClick(item)
        assertEquals(item, vm.selectedItem)
    }

    @Test
    fun clearSelectedItem_clearsSelection() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel()
        vm.onItemClick(testStorageItem())
        vm.clearSelectedItem()
        assertNull(vm.selectedItem)
    }

    @Test
    fun downloadItem_setsDownloadUrl() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(downloadUrl = "https://dl.example.com/f1")
        vm.downloadItem(testStorageItem("f1"))
        assertEquals("https://dl.example.com/f1", vm.downloadUrl)
    }

    @Test
    fun clearDownloadUrl_clearsDownloadUrl() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel()
        vm.downloadItem(testStorageItem())
        assertTrue(vm.downloadUrl != null)
        vm.clearDownloadUrl()
        assertNull(vm.downloadUrl)
    }

    @Test
    fun removeShare_success_reloadsItems() = ViewModelTestBase.runTestWithMain {
        val getShared = FakeGetSharedWithMeUseCase(Result.success(listOf(testShareLink())))
        val vm = SharedWithMeViewModel(
            getSharedWithMeUseCase = getShared,
            getItemUseCase = FakeGetItemUseCase(Result.success(testStorageItem())),
            deleteShareUseCase = FakeDeleteShareUseCase(Result.success(Unit)),
            getStorageUrlsUseCase = FakeGetStorageUrlsUseCase(),
        )
        vm.loadSharedItems()
        assertEquals(1, vm.sharedItems.size)
        getShared.result = Result.success(emptyList())
        vm.removeShare("share-1")
        assertTrue(vm.sharedItems.isEmpty())
        assertNull(vm.error)
    }

    @Test
    fun removeShare_error_setsError() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(
            getSharedWithMeResult = Result.success(listOf(testShareLink())),
            deleteShareResult = Result.error("ERR", "Delete failed"),
        )
        vm.loadSharedItems()
        vm.removeShare("share-1")
        assertEquals("Delete failed", vm.error)
    }

    @Test
    fun clearError_clearsErrorMessage() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getSharedWithMeResult = Result.error("ERR", "Oops"))
        vm.loadSharedItems()
        assertEquals("Oops", vm.error)
        vm.clearError()
        assertNull(vm.error)
    }
}
