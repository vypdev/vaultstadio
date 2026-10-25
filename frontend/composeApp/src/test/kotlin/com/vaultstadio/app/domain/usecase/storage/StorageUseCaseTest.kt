/**
 * Unit tests for storage use cases (CreateFolder, GetItem, Search).
 * Uses a fake StorageRepository to avoid platform/DI.
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.data.storage.usecase.BatchCopyUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.BatchDeleteUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.BatchMoveUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.BatchStarUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.CopyItemUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.CreateFolderUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.EmptyTrashUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.GetBreadcrumbsUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.GetFolderItemsUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.GetItemUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.GetRecentUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.GetStarredUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.GetTrashUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.MoveItemUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.RenameItemUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.RestoreItemUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.SearchUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.TrashItemUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.ToggleStarUseCaseImpl
import com.vaultstadio.app.data.storage.usecase.DeleteItemUseCaseImpl
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.model.BatchResult
import com.vaultstadio.app.domain.storage.model.Breadcrumb
import com.vaultstadio.app.domain.storage.model.ChunkedUploadInit
import com.vaultstadio.app.domain.storage.model.ChunkedUploadStatus
import com.vaultstadio.app.domain.storage.model.FolderUploadFile
import com.vaultstadio.app.domain.storage.model.FolderUploadResult
import com.vaultstadio.app.domain.storage.model.ItemType
import com.vaultstadio.app.domain.storage.model.PaginatedResponse
import com.vaultstadio.app.domain.storage.model.SortField
import com.vaultstadio.app.domain.storage.model.SortOrder
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.domain.storage.model.Visibility
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testStorageItem(
    id: String = "item-1",
    name: String = "Folder",
    type: ItemType = ItemType.FOLDER,
    parentId: String? = null,
) = StorageItem(
    id = id,
    name = name,
    path = "/$name",
    type = type,
    parentId = parentId,
    size = 0L,
    mimeType = null,
    visibility = Visibility.PRIVATE,
    isStarred = false,
    isTrashed = false,
    createdAt = testInstant,
    updatedAt = testInstant,
    metadata = null,
)

private fun <T> stubResult(): Result<T> = Result.error("TEST", "Not implemented in fake")

private fun testBreadcrumb(id: String?, name: String, path: String) = Breadcrumb(id = id, name = name, path = path)

private class FakeStorageRepository(
    var getItemsResult: Result<PaginatedResponse<StorageItem>> = Result.success(
        PaginatedResponse(emptyList(), 0L, 0, 50, 0, false),
    ),
    var createFolderResult: Result<StorageItem> = Result.success(testStorageItem()),
    var getItemResult: Result<StorageItem> = Result.success(testStorageItem()),
    var searchResult: Result<PaginatedResponse<StorageItem>> = Result.success(
        PaginatedResponse(emptyList(), 0L, 0, 50, 0, false),
    ),
    var getBreadcrumbsResult: Result<List<Breadcrumb>> = Result.success(emptyList()),
    var renameItemResult: Result<StorageItem> = Result.success(testStorageItem()),
    var moveItemResult: Result<StorageItem> = Result.success(testStorageItem()),
    var copyItemResult: Result<StorageItem> = Result.success(testStorageItem()),
    var toggleStarResult: Result<StorageItem> = Result.success(testStorageItem()),
    var trashItemResult: Result<StorageItem> = Result.success(testStorageItem()),
    var deleteItemPermanentlyResult: Result<Unit> = Result.success(Unit),
    var restoreItemResult: Result<StorageItem> = Result.success(testStorageItem()),
    var getTrashResult: Result<List<StorageItem>> = Result.success(emptyList()),
    var emptyTrashResult: Result<BatchResult> = Result.success(BatchResult(successful = 0, failed = 0)),
    var getStarredResult: Result<List<StorageItem>> = Result.success(emptyList()),
    var getRecentResult: Result<List<StorageItem>> = Result.success(emptyList()),
    var batchCopyResult: Result<BatchResult> = Result.success(BatchResult(successful = 0, failed = 0)),
    var batchDeleteResult: Result<BatchResult> = Result.success(BatchResult(successful = 0, failed = 0)),
    var batchMoveResult: Result<BatchResult> = Result.success(BatchResult(successful = 0, failed = 0)),
    var batchStarResult: Result<BatchResult> = Result.success(BatchResult(successful = 0, failed = 0)),
) : StorageRepository {

    override suspend fun getItems(
        folderId: String?,
        sortBy: SortField,
        sortOrder: SortOrder,
        limit: Int,
        offset: Int,
    ): Result<PaginatedResponse<StorageItem>> = getItemsResult

    override suspend fun getItem(itemId: String): Result<StorageItem> = getItemResult

    override suspend fun createFolder(name: String, parentId: String?): Result<StorageItem> = createFolderResult

    override suspend fun getBreadcrumbs(itemId: String): Result<List<Breadcrumb>> = getBreadcrumbsResult

    override suspend fun renameItem(itemId: String, newName: String): Result<StorageItem> = renameItemResult

    override suspend fun moveItem(itemId: String, destinationId: String?, newName: String?): Result<StorageItem> =
        moveItemResult

    override suspend fun copyItem(itemId: String, destinationId: String?, newName: String?): Result<StorageItem> =
        copyItemResult

    override suspend fun toggleStar(itemId: String): Result<StorageItem> = toggleStarResult

    override suspend fun trashItem(itemId: String): Result<StorageItem> = trashItemResult

    override suspend fun deleteItemPermanently(itemId: String): Result<Unit> = deleteItemPermanentlyResult

    override suspend fun restoreItem(itemId: String): Result<StorageItem> = restoreItemResult

    override suspend fun getTrash(): Result<List<StorageItem>> = getTrashResult

    override suspend fun emptyTrash(): Result<BatchResult> = emptyTrashResult

    override suspend fun getStarred(): Result<List<StorageItem>> = getStarredResult

    override suspend fun getRecent(limit: Int): Result<List<StorageItem>> = getRecentResult

    override suspend fun search(query: String, limit: Int, offset: Int): Result<PaginatedResponse<StorageItem>> =
        searchResult

    override suspend fun batchCopy(itemIds: List<String>, destinationId: String?): Result<BatchResult> =
        batchCopyResult

    override suspend fun batchDelete(itemIds: List<String>, permanent: Boolean): Result<BatchResult> = batchDeleteResult

    override suspend fun batchMove(itemIds: List<String>, destinationId: String?): Result<BatchResult> =
        batchMoveResult

    override suspend fun batchStar(itemIds: List<String>, starred: Boolean): Result<BatchResult> = batchStarResult

    override suspend fun uploadFile(
        fileName: String,
        fileData: ByteArray,
        mimeType: String,
        parentId: String?,
        onProgress: (Float) -> Unit,
    ): Result<StorageItem> = stubResult()

    override suspend fun uploadFolder(
        files: List<FolderUploadFile>,
        parentId: String?,
        onProgress: (Float) -> Unit,
    ): Result<FolderUploadResult> = stubResult()

    override suspend fun initChunkedUpload(
        fileName: String,
        totalSize: Long,
        mimeType: String?,
        parentId: String?,
        chunkSize: Long,
    ): Result<ChunkedUploadInit> = stubResult()

    override suspend fun uploadChunk(
        uploadId: String,
        chunkIndex: Int,
        chunkData: ByteArray,
    ): Result<ChunkedUploadStatus> = stubResult()

    override suspend fun getUploadStatus(uploadId: String): Result<ChunkedUploadStatus> = stubResult()

    override suspend fun completeChunkedUpload(uploadId: String): Result<StorageItem> = stubResult()

    override suspend fun cancelChunkedUpload(uploadId: String): Result<Unit> = stubResult()

    override suspend fun downloadFile(itemId: String): Result<ByteArray> = stubResult()

    override fun getDownloadUrl(itemId: String): String = ""

    override fun getThumbnailUrl(itemId: String, size: String): String = ""

    override fun getPreviewUrl(itemId: String): String = ""
}

class CreateFolderUseCaseTest {

    @Test
    fun invoke_returnsRepositoryCreateFolderResult() = runTest {
        val folder = testStorageItem(id = "f1", name = "NewFolder", parentId = "root")
        val repo = FakeStorageRepository(createFolderResult = Result.success(folder))
        val useCase = CreateFolderUseCaseImpl(repo)
        val result = useCase("NewFolder", "root")
        assertTrue(result.isSuccess())
        assertEquals(folder, result.getOrNull())
    }

    @Test
    fun invoke_withNullParentId_forwardsToRepository() = runTest {
        val folder = testStorageItem(parentId = null)
        val repo = FakeStorageRepository(createFolderResult = Result.success(folder))
        val useCase = CreateFolderUseCaseImpl(repo)
        val result = useCase("RootFolder", null)
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(createFolderResult = Result.error("CONFLICT", "Name already exists"))
        val useCase = CreateFolderUseCaseImpl(repo)
        val result = useCase("Existing", null)
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class GetItemUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetItemResult() = runTest {
        val item = testStorageItem(id = "i1", name = "file.pdf", type = ItemType.FILE)
        val repo = FakeStorageRepository(getItemResult = Result.success(item))
        val useCase = GetItemUseCaseImpl(repo)
        val result = useCase("i1")
        assertTrue(result.isSuccess())
        assertEquals(item, result.getOrNull())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(getItemResult = Result.error("NOT_FOUND", "Item not found"))
        val useCase = GetItemUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
    }
}

class SearchUseCaseTest {

    @Test
    fun invoke_returnsRepositorySearchResult() = runTest {
        val items = listOf(testStorageItem("s1", "match.pdf", ItemType.FILE))
        val paged = PaginatedResponse(items, 1L, 0, 50, 1, false)
        val repo = FakeStorageRepository(searchResult = Result.success(paged))
        val useCase = SearchUseCaseImpl(repo)
        val result = useCase("query", limit = 50, offset = 0)
        assertTrue(result.isSuccess())
        assertEquals(1, result.getOrNull()?.items?.size)
        assertEquals(1L, result.getOrNull()?.total)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(searchResult = Result.error("UNAUTHORIZED", "Not logged in"))
        val useCase = SearchUseCaseImpl(repo)
        val result = useCase("q")
        assertTrue(result.isError())
    }
}

class GetBreadcrumbsUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetBreadcrumbsResult() = runTest {
        val breadcrumbs = listOf(
            testBreadcrumb(null, "Root", "/"),
            testBreadcrumb("f1", "Documents", "/Documents"),
            testBreadcrumb("f2", "Report.pdf", "/Documents/Report.pdf"),
        )
        val repo = FakeStorageRepository(getBreadcrumbsResult = Result.success(breadcrumbs))
        val useCase = GetBreadcrumbsUseCaseImpl(repo)
        val result = useCase("item-1")
        assertTrue(result.isSuccess())
        assertEquals(3, result.getOrNull()?.size)
        assertEquals("Root", result.getOrNull()?.get(0)?.name)
        assertEquals("/Documents/Report.pdf", result.getOrNull()?.get(2)?.path)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(getBreadcrumbsResult = Result.error("NOT_FOUND", "Item not found"))
        val useCase = GetBreadcrumbsUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
    }

    @Test
    fun invoke_returnsEmptyListWhenRepositoryReturnsEmpty() = runTest {
        val repo = FakeStorageRepository(getBreadcrumbsResult = Result.success(emptyList()))
        val useCase = GetBreadcrumbsUseCaseImpl(repo)
        val result = useCase("root-item")
        assertTrue(result.isSuccess())
        assertEquals(0, result.getOrNull()?.size)
    }
}

class MoveItemUseCaseTest {

    @Test
    fun invoke_returnsRepositoryMoveItemResult() = runTest {
        val item = testStorageItem(id = "moved-1", name = "Moved.pdf", parentId = "dest-folder")
        val repo = FakeStorageRepository(moveItemResult = Result.success(item))
        val useCase = MoveItemUseCaseImpl(repo)
        val result = useCase("item-1", "dest-folder", null)
        assertTrue(result.isSuccess())
        assertEquals("dest-folder", result.getOrNull()?.parentId)
    }

    @Test
    fun invoke_withNewName_forwardsToRepository() = runTest {
        val item = testStorageItem(name = "Renamed.pdf")
        val repo = FakeStorageRepository(moveItemResult = Result.success(item))
        val useCase = MoveItemUseCaseImpl(repo)
        val result = useCase("item-1", "dest-id", "Renamed.pdf")
        assertTrue(result.isSuccess())
        assertEquals("Renamed.pdf", result.getOrNull()?.name)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(moveItemResult = Result.error("CONFLICT", "Name already exists"))
        val useCase = MoveItemUseCaseImpl(repo)
        val result = useCase("item-1", "dest", null)
        assertTrue(result.isError())
    }
}

class CopyItemUseCaseTest {

    @Test
    fun invoke_returnsRepositoryCopyItemResult() = runTest {
        val item = testStorageItem(id = "copy-1", name = "Copy.pdf", parentId = "dest-folder")
        val repo = FakeStorageRepository(copyItemResult = Result.success(item))
        val useCase = CopyItemUseCaseImpl(repo)
        val result = useCase("item-1", "dest-folder", null)
        assertTrue(result.isSuccess())
        assertEquals("dest-folder", result.getOrNull()?.parentId)
    }

    @Test
    fun invoke_withNewName_forwardsToRepository() = runTest {
        val item = testStorageItem(name = "Copied.pdf")
        val repo = FakeStorageRepository(copyItemResult = Result.success(item))
        val useCase = CopyItemUseCaseImpl(repo)
        val result = useCase("item-1", "dest-id", "Copied.pdf")
        assertTrue(result.isSuccess())
        assertEquals("Copied.pdf", result.getOrNull()?.name)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(copyItemResult = Result.error("FORBIDDEN", "No write access"))
        val useCase = CopyItemUseCaseImpl(repo)
        val result = useCase("item-1", "dest", null)
        assertTrue(result.isError())
    }
}

class BatchCopyUseCaseTest {

    @Test
    fun invoke_returnsRepositoryBatchCopyResult() = runTest {
        val batchResult = BatchResult(successful = 2, failed = 0)
        val repo = FakeStorageRepository(batchCopyResult = Result.success(batchResult))
        val useCase = BatchCopyUseCaseImpl(repo)
        val result = useCase(listOf("item-1", "item-2"), "dest-folder")
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.successful)
        assertEquals(0, result.getOrNull()?.failed)
    }

    @Test
    fun invoke_withNullDestination_forwardsToRepository() = runTest {
        val batchResult = BatchResult(successful = 1, failed = 0)
        val repo = FakeStorageRepository(batchCopyResult = Result.success(batchResult))
        val useCase = BatchCopyUseCaseImpl(repo)
        val result = useCase(listOf("item-1"), null)
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(batchCopyResult = Result.error("CONFLICT", "Destination not found"))
        val useCase = BatchCopyUseCaseImpl(repo)
        val result = useCase(listOf("item-1"), "dest")
        assertTrue(result.isError())
    }
}

class GetFolderItemsUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetItemsResult() = runTest {
        val items = listOf(
            testStorageItem("i1", "File1", ItemType.FILE),
            testStorageItem("i2", "Folder1", ItemType.FOLDER),
        )
        val paged = PaginatedResponse(items, 2L, 0, 50, 2, false)
        val repo = FakeStorageRepository(getItemsResult = Result.success(paged))
        val useCase = GetFolderItemsUseCaseImpl(repo)
        val result = useCase("folder-id", SortField.NAME, SortOrder.ASC, 50, 0)
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.items?.size)
        assertEquals(2L, result.getOrNull()?.total)
    }

    @Test
    fun invoke_withNullFolderId_forwardsToRepository() = runTest {
        val repo = FakeStorageRepository(getItemsResult = Result.success(PaginatedResponse(emptyList(), 0L, 0, 50, 0, false)))
        val useCase = GetFolderItemsUseCaseImpl(repo)
        val result = useCase(null, SortField.UPDATED_AT, SortOrder.DESC, 20, 0)
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(getItemsResult = Result.error("UNAUTHORIZED", "Not logged in"))
        val useCase = GetFolderItemsUseCaseImpl(repo)
        val result = useCase(null, SortField.NAME, SortOrder.ASC, 50, 0)
        assertTrue(result.isError())
    }
}

class RenameItemUseCaseTest {

    @Test
    fun invoke_returnsRepositoryRenameItemResult() = runTest {
        val item = testStorageItem(id = "i1", name = "NewName.pdf")
        val repo = FakeStorageRepository(renameItemResult = Result.success(item))
        val useCase = RenameItemUseCaseImpl(repo)
        val result = useCase("i1", "NewName.pdf")
        assertTrue(result.isSuccess())
        assertEquals("NewName.pdf", result.getOrNull()?.name)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(renameItemResult = Result.error("CONFLICT", "Name exists"))
        val useCase = RenameItemUseCaseImpl(repo)
        val result = useCase("i1", "Existing")
        assertTrue(result.isError())
    }
}

class ToggleStarUseCaseTest {

    @Test
    fun invoke_returnsRepositoryToggleStarResult() = runTest {
        val item = testStorageItem(id = "i1", name = "Starred").copy(isStarred = true)
        val repo = FakeStorageRepository(toggleStarResult = Result.success(item))
        val useCase = ToggleStarUseCaseImpl(repo)
        val result = useCase("i1")
        assertTrue(result.isSuccess())
        assertTrue(result.getOrNull()?.isStarred == true)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(toggleStarResult = Result.error("NOT_FOUND", "Item not found"))
        val useCase = ToggleStarUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
    }
}

class TrashItemUseCaseTest {

    @Test
    fun invoke_returnsRepositoryTrashItemResult() = runTest {
        val item = testStorageItem(id = "i1", name = "Trashed").copy(isTrashed = true)
        val repo = FakeStorageRepository(trashItemResult = Result.success(item))
        val useCase = TrashItemUseCaseImpl(repo)
        val result = useCase("i1")
        assertTrue(result.isSuccess())
        assertTrue(result.getOrNull()?.isTrashed == true)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(trashItemResult = Result.error("FORBIDDEN", "Cannot trash"))
        val useCase = TrashItemUseCaseImpl(repo)
        val result = useCase("i1")
        assertTrue(result.isError())
    }
}

class RestoreItemUseCaseTest {

    @Test
    fun invoke_returnsRepositoryRestoreItemResult() = runTest {
        val item = testStorageItem(id = "i1", name = "Restored")
        val repo = FakeStorageRepository(restoreItemResult = Result.success(item))
        val useCase = RestoreItemUseCaseImpl(repo)
        val result = useCase("i1")
        assertTrue(result.isSuccess())
        assertTrue(result.getOrNull()?.isTrashed == false)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(restoreItemResult = Result.error("NOT_FOUND", "Item not found"))
        val useCase = RestoreItemUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
    }
}

class GetTrashUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetTrashResult() = runTest {
        val items = listOf(testStorageItem("i1", "Trashed.pdf", ItemType.FILE).copy(isTrashed = true))
        val repo = FakeStorageRepository(getTrashResult = Result.success(items))
        val useCase = GetTrashUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun invoke_returnsEmptyListWhenNoTrash() = runTest {
        val repo = FakeStorageRepository(getTrashResult = Result.success(emptyList()))
        val useCase = GetTrashUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(getTrashResult = Result.error("UNAUTHORIZED", "Not logged in"))
        val useCase = GetTrashUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
    }
}

class GetStarredUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetStarredResult() = runTest {
        val items = listOf(testStorageItem("i1", "Starred.pdf", ItemType.FILE).copy(isStarred = true))
        val repo = FakeStorageRepository(getStarredResult = Result.success(items))
        val useCase = GetStarredUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(getStarredResult = Result.error("UNAUTHORIZED", "Not logged in"))
        val useCase = GetStarredUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
    }
}

class GetRecentUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetRecentResult() = runTest {
        val items = listOf(testStorageItem("i1", "Recent.pdf", ItemType.FILE))
        val repo = FakeStorageRepository(getRecentResult = Result.success(items))
        val useCase = GetRecentUseCaseImpl(repo)
        val result = useCase(10)
        assertTrue(result.isSuccess())
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(getRecentResult = Result.error("UNAUTHORIZED", "Not logged in"))
        val useCase = GetRecentUseCaseImpl(repo)
        val result = useCase(20)
        assertTrue(result.isError())
    }
}

class EmptyTrashUseCaseTest {

    @Test
    fun invoke_returnsRepositoryEmptyTrashResult() = runTest {
        val batchResult = BatchResult(successful = 3, failed = 0)
        val repo = FakeStorageRepository(emptyTrashResult = Result.success(batchResult))
        val useCase = EmptyTrashUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
        assertEquals(3, result.getOrNull()?.successful)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(emptyTrashResult = Result.error("FORBIDDEN", "Cannot empty trash"))
        val useCase = EmptyTrashUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
    }
}

class DeleteItemUseCaseTest {

    @Test
    fun invoke_returnsRepositoryDeleteItemPermanentlyResult() = runTest {
        val repo = FakeStorageRepository(deleteItemPermanentlyResult = Result.success(Unit))
        val useCase = DeleteItemUseCaseImpl(repo)
        val result = useCase("i1")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(deleteItemPermanentlyResult = Result.error("NOT_FOUND", "Item not found"))
        val useCase = DeleteItemUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
    }
}

class BatchDeleteUseCaseTest {

    @Test
    fun invoke_returnsRepositoryBatchDeleteResult() = runTest {
        val batchResult = BatchResult(successful = 2, failed = 0)
        val repo = FakeStorageRepository(batchDeleteResult = Result.success(batchResult))
        val useCase = BatchDeleteUseCaseImpl(repo)
        val result = useCase(listOf("i1", "i2"), permanent = false)
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.successful)
    }

    @Test
    fun invoke_withPermanentTrue_forwardsToRepository() = runTest {
        val batchResult = BatchResult(successful = 1, failed = 0)
        val repo = FakeStorageRepository(batchDeleteResult = Result.success(batchResult))
        val useCase = BatchDeleteUseCaseImpl(repo)
        val result = useCase(listOf("i1"), permanent = true)
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(batchDeleteResult = Result.error("FORBIDDEN", "No permission"))
        val useCase = BatchDeleteUseCaseImpl(repo)
        val result = useCase(listOf("i1"), false)
        assertTrue(result.isError())
    }
}

class BatchMoveUseCaseTest {

    @Test
    fun invoke_returnsRepositoryBatchMoveResult() = runTest {
        val batchResult = BatchResult(successful = 2, failed = 0)
        val repo = FakeStorageRepository(batchMoveResult = Result.success(batchResult))
        val useCase = BatchMoveUseCaseImpl(repo)
        val result = useCase(listOf("i1", "i2"), "dest-folder")
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.successful)
    }

    @Test
    fun invoke_withNullDestination_forwardsToRepository() = runTest {
        val batchResult = BatchResult(successful = 1, failed = 0)
        val repo = FakeStorageRepository(batchMoveResult = Result.success(batchResult))
        val useCase = BatchMoveUseCaseImpl(repo)
        val result = useCase(listOf("i1"), null)
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(batchMoveResult = Result.error("CONFLICT", "Destination not found"))
        val useCase = BatchMoveUseCaseImpl(repo)
        val result = useCase(listOf("i1"), "dest")
        assertTrue(result.isError())
    }
}

class BatchStarUseCaseTest {

    @Test
    fun invoke_returnsRepositoryBatchStarResult() = runTest {
        val batchResult = BatchResult(successful = 2, failed = 0)
        val repo = FakeStorageRepository(batchStarResult = Result.success(batchResult))
        val useCase = BatchStarUseCaseImpl(repo)
        val result = useCase(listOf("i1", "i2"), starred = true)
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.successful)
    }

    @Test
    fun invoke_withStarredFalse_forwardsToRepository() = runTest {
        val batchResult = BatchResult(successful = 1, failed = 0)
        val repo = FakeStorageRepository(batchStarResult = Result.success(batchResult))
        val useCase = BatchStarUseCaseImpl(repo)
        val result = useCase(listOf("i1"), starred = false)
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(batchStarResult = Result.error("NOT_FOUND", "Item not found"))
        val useCase = BatchStarUseCaseImpl(repo)
        val result = useCase(listOf("i1"), true)
        assertTrue(result.isError())
    }
}
