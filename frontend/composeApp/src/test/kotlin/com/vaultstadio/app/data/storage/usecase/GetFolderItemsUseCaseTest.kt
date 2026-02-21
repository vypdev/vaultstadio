/**
 * Unit tests for GetFolderItemsUseCaseImpl.
 * Uses a fake StorageRepository to avoid network/DI.
 */

package com.vaultstadio.app.data.storage.usecase

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
import kotlin.test.assertTrue

private fun stubItem(id: String, name: String): StorageItem =
    StorageItem(
        id = id,
        name = name,
        path = "/$name",
        type = ItemType.FILE,
        parentId = null,
        size = 0L,
        mimeType = null,
        visibility = Visibility.PRIVATE,
        isStarred = false,
        isTrashed = false,
        createdAt = Instant.DISTANT_PAST,
        updatedAt = Instant.DISTANT_PAST,
        metadata = null,
    )

private class FakeStorageRepository(
    private val response: Result<PaginatedResponse<StorageItem>>,
) : StorageRepository {
    override suspend fun getItems(
        folderId: String?,
        sortBy: SortField,
        sortOrder: SortOrder,
        limit: Int,
        offset: Int,
    ): Result<PaginatedResponse<StorageItem>> = response

    override suspend fun getItem(itemId: String): Result<StorageItem> =
        Result.Error("not-implemented", "Fake")

    override suspend fun createFolder(name: String, parentId: String?): Result<StorageItem> =
        Result.Error("not-implemented", "Fake")

    override suspend fun getBreadcrumbs(itemId: String): Result<List<Breadcrumb>> =
        Result.Error("not-implemented", "Fake")

    override suspend fun renameItem(itemId: String, newName: String): Result<StorageItem> =
        Result.Error("not-implemented", "Fake")

    override suspend fun moveItem(
        itemId: String,
        destinationId: String?,
        newName: String?,
    ): Result<StorageItem> = Result.Error("not-implemented", "Fake")

    override suspend fun copyItem(
        itemId: String,
        destinationId: String?,
        newName: String?,
    ): Result<StorageItem> = Result.Error("not-implemented", "Fake")

    override suspend fun toggleStar(itemId: String): Result<StorageItem> =
        Result.Error("not-implemented", "Fake")

    override suspend fun trashItem(itemId: String): Result<StorageItem> =
        Result.Error("not-implemented", "Fake")

    override suspend fun deleteItemPermanently(itemId: String): Result<Unit> =
        Result.Error("not-implemented", "Fake")

    override suspend fun restoreItem(itemId: String): Result<StorageItem> =
        Result.Error("not-implemented", "Fake")

    override suspend fun getTrash(): Result<List<StorageItem>> =
        Result.Error("not-implemented", "Fake")

    override suspend fun emptyTrash(): Result<BatchResult> =
        Result.Error("not-implemented", "Fake")

    override suspend fun getStarred(): Result<List<StorageItem>> =
        Result.Error("not-implemented", "Fake")

    override suspend fun getRecent(limit: Int): Result<List<StorageItem>> =
        Result.Error("not-implemented", "Fake")

    override suspend fun search(
        query: String,
        limit: Int,
        offset: Int,
    ): Result<PaginatedResponse<StorageItem>> =
        Result.Error("not-implemented", "Fake")

    override suspend fun batchDelete(
        itemIds: List<String>,
        permanent: Boolean,
    ): Result<BatchResult> =
        Result.Error("not-implemented", "Fake")

    override suspend fun batchMove(
        itemIds: List<String>,
        destinationId: String?,
    ): Result<BatchResult> =
        Result.Error("not-implemented", "Fake")

    override suspend fun batchCopy(
        itemIds: List<String>,
        destinationId: String?,
    ): Result<BatchResult> =
        Result.Error("not-implemented", "Fake")

    override suspend fun batchStar(
        itemIds: List<String>,
        starred: Boolean,
    ): Result<BatchResult> =
        Result.Error("not-implemented", "Fake")

    override suspend fun uploadFile(
        fileName: String,
        fileData: ByteArray,
        mimeType: String,
        parentId: String?,
        onProgress: (Float) -> Unit,
    ): Result<StorageItem> = Result.Error("not-implemented", "Fake")

    override suspend fun uploadFolder(
        files: List<FolderUploadFile>,
        parentId: String?,
        onProgress: (Float) -> Unit,
    ): Result<FolderUploadResult> =
        Result.Error("not-implemented", "Fake")

    override suspend fun initChunkedUpload(
        fileName: String,
        totalSize: Long,
        mimeType: String?,
        parentId: String?,
        chunkSize: Long,
    ): Result<ChunkedUploadInit> =
        Result.Error("not-implemented", "Fake")

    override suspend fun uploadChunk(
        uploadId: String,
        chunkIndex: Int,
        chunkData: ByteArray,
    ): Result<ChunkedUploadStatus> =
        Result.Error("not-implemented", "Fake")

    override suspend fun getUploadStatus(uploadId: String): Result<ChunkedUploadStatus> =
        Result.Error("not-implemented", "Fake")

    override suspend fun completeChunkedUpload(uploadId: String): Result<StorageItem> =
        Result.Error("not-implemented", "Fake")

    override suspend fun cancelChunkedUpload(uploadId: String): Result<Unit> =
        Result.Error("not-implemented", "Fake")

    override suspend fun downloadFile(itemId: String): Result<ByteArray> =
        Result.Error("not-implemented", "Fake")

    override fun getDownloadUrl(itemId: String): String = ""

    override fun getThumbnailUrl(itemId: String, size: String): String = ""

    override fun getPreviewUrl(itemId: String): String = ""
}

class GetFolderItemsUseCaseTest {

    @Test
    fun invoke_returnsSuccessFromRepository() = runTest {
        val items = listOf(stubItem("1", "a.txt"), stubItem("2", "b.txt"))
        val paginated = PaginatedResponse(
            items = items,
            total = 2L,
            page = 1,
            pageSize = 100,
            totalPages = 1,
            hasMore = false,
        )
        val repo = FakeStorageRepository(Result.Success(paginated))
        val useCase = GetFolderItemsUseCaseImpl(repo)

        val result = useCase.invoke(folderId = null, sortBy = SortField.NAME, sortOrder = SortOrder.ASC, limit = 100, offset = 0)

        assertTrue(result.isSuccess())
        val success = result as Result.Success
        assertEquals(2, success.data.items.size)
        assertEquals("a.txt", success.data.items[0].name)
        assertEquals("b.txt", success.data.items[1].name)
    }

    @Test
    fun invoke_forwardsErrorFromRepository() = runTest {
        val repo = FakeStorageRepository(Result.Error("err", "message"))
        val useCase = GetFolderItemsUseCaseImpl(repo)

        val result = useCase.invoke()

        assertTrue(result.isError())
        val err = result as Result.Error
        assertEquals("err", err.code)
        assertEquals("message", err.message)
    }
}
