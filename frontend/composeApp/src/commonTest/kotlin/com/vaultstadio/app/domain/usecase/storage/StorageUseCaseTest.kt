/**
 * Unit tests for storage use cases (CreateFolder, GetItem, Search).
 * Uses a fake StorageRepository to avoid platform/DI.
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.BatchResult
import com.vaultstadio.app.domain.model.Breadcrumb
import com.vaultstadio.app.domain.model.ChunkedUploadInit
import com.vaultstadio.app.domain.model.ChunkedUploadStatus
import com.vaultstadio.app.domain.model.FolderUploadFile
import com.vaultstadio.app.domain.model.FolderUploadResult
import com.vaultstadio.app.domain.model.ItemType
import com.vaultstadio.app.domain.model.PaginatedResponse
import com.vaultstadio.app.domain.model.SortField
import com.vaultstadio.app.domain.model.SortOrder
import com.vaultstadio.app.domain.model.StorageItem
import com.vaultstadio.app.domain.model.Visibility
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

private fun <T> stubResult(): ApiResult<T> = ApiResult.error("TEST", "Not implemented in fake")

private fun testBreadcrumb(id: String?, name: String, path: String) = Breadcrumb(id = id, name = name, path = path)

private class FakeStorageRepository(
    var createFolderResult: ApiResult<StorageItem> = ApiResult.success(testStorageItem()),
    var getItemResult: ApiResult<StorageItem> = ApiResult.success(testStorageItem()),
    var searchResult: ApiResult<PaginatedResponse<StorageItem>> = ApiResult.success(
        PaginatedResponse(emptyList(), 0L, 0, 50, 0, false),
    ),
    var getBreadcrumbsResult: ApiResult<List<Breadcrumb>> = ApiResult.success(emptyList()),
    var moveItemResult: ApiResult<StorageItem> = ApiResult.success(testStorageItem()),
) : StorageRepository {

    override suspend fun getItems(
        folderId: String?,
        sortBy: SortField,
        sortOrder: SortOrder,
        limit: Int,
        offset: Int,
    ): ApiResult<PaginatedResponse<StorageItem>> = stubResult()

    override suspend fun getItem(itemId: String): ApiResult<StorageItem> = getItemResult

    override suspend fun createFolder(name: String, parentId: String?): ApiResult<StorageItem> = createFolderResult

    override suspend fun getBreadcrumbs(itemId: String): ApiResult<List<Breadcrumb>> = getBreadcrumbsResult

    override suspend fun renameItem(itemId: String, newName: String): ApiResult<StorageItem> = stubResult()

    override suspend fun moveItem(itemId: String, destinationId: String?, newName: String?): ApiResult<StorageItem> =
        moveItemResult

    override suspend fun copyItem(itemId: String, destinationId: String?, newName: String?): ApiResult<StorageItem> =
        stubResult()

    override suspend fun toggleStar(itemId: String): ApiResult<StorageItem> = stubResult()

    override suspend fun trashItem(itemId: String): ApiResult<StorageItem> = stubResult()

    override suspend fun deleteItemPermanently(itemId: String): ApiResult<Unit> = stubResult()

    override suspend fun restoreItem(itemId: String): ApiResult<StorageItem> = stubResult()

    override suspend fun getTrash(): ApiResult<List<StorageItem>> = stubResult()

    override suspend fun emptyTrash(): ApiResult<BatchResult> = stubResult()

    override suspend fun getStarred(): ApiResult<List<StorageItem>> = stubResult()

    override suspend fun getRecent(limit: Int): ApiResult<List<StorageItem>> = stubResult()

    override suspend fun search(query: String, limit: Int, offset: Int): ApiResult<PaginatedResponse<StorageItem>> =
        searchResult

    override suspend fun batchDelete(itemIds: List<String>, permanent: Boolean): ApiResult<BatchResult> = stubResult()

    override suspend fun batchMove(itemIds: List<String>, destinationId: String?): ApiResult<BatchResult> = stubResult()

    override suspend fun batchCopy(itemIds: List<String>, destinationId: String?): ApiResult<BatchResult> = stubResult()

    override suspend fun batchStar(itemIds: List<String>, starred: Boolean): ApiResult<BatchResult> = stubResult()

    override suspend fun uploadFile(
        fileName: String,
        fileData: ByteArray,
        mimeType: String,
        parentId: String?,
        onProgress: (Float) -> Unit,
    ): ApiResult<StorageItem> = stubResult()

    override suspend fun uploadFolder(
        files: List<FolderUploadFile>,
        parentId: String?,
        onProgress: (Float) -> Unit,
    ): ApiResult<FolderUploadResult> = stubResult()

    override suspend fun initChunkedUpload(
        fileName: String,
        totalSize: Long,
        mimeType: String?,
        parentId: String?,
        chunkSize: Long,
    ): ApiResult<ChunkedUploadInit> = stubResult()

    override suspend fun uploadChunk(
        uploadId: String,
        chunkIndex: Int,
        chunkData: ByteArray,
    ): ApiResult<ChunkedUploadStatus> = stubResult()

    override suspend fun getUploadStatus(uploadId: String): ApiResult<ChunkedUploadStatus> = stubResult()

    override suspend fun completeChunkedUpload(uploadId: String): ApiResult<StorageItem> = stubResult()

    override suspend fun cancelChunkedUpload(uploadId: String): ApiResult<Unit> = stubResult()

    override suspend fun downloadFile(itemId: String): ApiResult<ByteArray> = stubResult()

    override fun getDownloadUrl(itemId: String): String = ""

    override fun getThumbnailUrl(itemId: String, size: String): String = ""

    override fun getPreviewUrl(itemId: String): String = ""
}

class CreateFolderUseCaseTest {

    @Test
    fun invoke_returnsRepositoryCreateFolderResult() = runTest {
        val folder = testStorageItem(id = "f1", name = "NewFolder", parentId = "root")
        val repo = FakeStorageRepository(createFolderResult = ApiResult.success(folder))
        val useCase = CreateFolderUseCaseImpl(repo)
        val result = useCase("NewFolder", "root")
        assertTrue(result.isSuccess())
        assertEquals(folder, result.getOrNull())
    }

    @Test
    fun invoke_withNullParentId_forwardsToRepository() = runTest {
        val folder = testStorageItem(parentId = null)
        val repo = FakeStorageRepository(createFolderResult = ApiResult.success(folder))
        val useCase = CreateFolderUseCaseImpl(repo)
        val result = useCase("RootFolder", null)
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(createFolderResult = ApiResult.error("CONFLICT", "Name already exists"))
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
        val repo = FakeStorageRepository(getItemResult = ApiResult.success(item))
        val useCase = GetItemUseCaseImpl(repo)
        val result = useCase("i1")
        assertTrue(result.isSuccess())
        assertEquals(item, result.getOrNull())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(getItemResult = ApiResult.error("NOT_FOUND", "Item not found"))
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
        val repo = FakeStorageRepository(searchResult = ApiResult.success(paged))
        val useCase = SearchUseCaseImpl(repo)
        val result = useCase("query", limit = 50, offset = 0)
        assertTrue(result.isSuccess())
        assertEquals(1, result.getOrNull()?.items?.size)
        assertEquals(1L, result.getOrNull()?.total)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(searchResult = ApiResult.error("UNAUTHORIZED", "Not logged in"))
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
        val repo = FakeStorageRepository(getBreadcrumbsResult = ApiResult.success(breadcrumbs))
        val useCase = GetBreadcrumbsUseCaseImpl(repo)
        val result = useCase("item-1")
        assertTrue(result.isSuccess())
        assertEquals(3, result.getOrNull()?.size)
        assertEquals("Root", result.getOrNull()?.get(0)?.name)
        assertEquals("/Documents/Report.pdf", result.getOrNull()?.get(2)?.path)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(getBreadcrumbsResult = ApiResult.error("NOT_FOUND", "Item not found"))
        val useCase = GetBreadcrumbsUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
    }
}

class MoveItemUseCaseTest {

    @Test
    fun invoke_returnsRepositoryMoveItemResult() = runTest {
        val item = testStorageItem(id = "moved-1", name = "Moved.pdf", parentId = "dest-folder")
        val repo = FakeStorageRepository(moveItemResult = ApiResult.success(item))
        val useCase = MoveItemUseCaseImpl(repo)
        val result = useCase("item-1", "dest-folder", null)
        assertTrue(result.isSuccess())
        assertEquals("dest-folder", result.getOrNull()?.parentId)
    }

    @Test
    fun invoke_withNewName_forwardsToRepository() = runTest {
        val item = testStorageItem(name = "Renamed.pdf")
        val repo = FakeStorageRepository(moveItemResult = ApiResult.success(item))
        val useCase = MoveItemUseCaseImpl(repo)
        val result = useCase("item-1", "dest-id", "Renamed.pdf")
        assertTrue(result.isSuccess())
        assertEquals("Renamed.pdf", result.getOrNull()?.name)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeStorageRepository(moveItemResult = ApiResult.error("CONFLICT", "Name already exists"))
        val useCase = MoveItemUseCaseImpl(repo)
        val result = useCase("item-1", "dest", null)
        assertTrue(result.isError())
    }
}
