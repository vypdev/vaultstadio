/**
 * Unit tests for UploadManager: upload destination and minimized state.
 * Repository is not exercised; only state setters and getters are tested.
 */

package com.vaultstadio.app.feature.upload

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.BatchResult
import com.vaultstadio.app.domain.model.Breadcrumb
import com.vaultstadio.app.domain.model.ChunkedUploadInit
import com.vaultstadio.app.domain.model.ChunkedUploadStatus
import com.vaultstadio.app.domain.model.FolderUploadFile
import com.vaultstadio.app.domain.model.FolderUploadResult
import com.vaultstadio.app.domain.model.PaginatedResponse
import com.vaultstadio.app.domain.model.SortField
import com.vaultstadio.app.domain.model.SortOrder
import com.vaultstadio.app.domain.model.StorageItem
import com.vaultstadio.app.feature.ViewModelTestBase
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Stub repository used when testing UploadManager state only (no uploads). */
private class StubStorageRepository : StorageRepository {
    override suspend fun getItems(
        folderId: String?,
        sortBy: SortField,
        sortOrder: SortOrder,
        limit: Int,
        offset: Int,
    ): ApiResult<PaginatedResponse<StorageItem>> = ApiResult.Error("", "")

    override suspend fun getItem(itemId: String): ApiResult<StorageItem> = ApiResult.Error("", "")
    override suspend fun createFolder(name: String, parentId: String?): ApiResult<StorageItem> =
        ApiResult.Error("", "")

    override suspend fun getBreadcrumbs(itemId: String): ApiResult<List<Breadcrumb>> = ApiResult.Error("", "")
    override suspend fun renameItem(itemId: String, newName: String): ApiResult<StorageItem> = ApiResult.Error("", "")
    override suspend fun moveItem(
        itemId: String,
        destinationId: String?,
        newName: String?,
    ): ApiResult<StorageItem> = ApiResult.Error("", "")

    override suspend fun copyItem(
        itemId: String,
        destinationId: String?,
        newName: String?,
    ): ApiResult<StorageItem> = ApiResult.Error("", "")

    override suspend fun toggleStar(itemId: String): ApiResult<StorageItem> = ApiResult.Error("", "")
    override suspend fun trashItem(itemId: String): ApiResult<StorageItem> = ApiResult.Error("", "")
    override suspend fun deleteItemPermanently(itemId: String): ApiResult<Unit> = ApiResult.Error("", "")
    override suspend fun restoreItem(itemId: String): ApiResult<StorageItem> = ApiResult.Error("", "")
    override suspend fun getTrash(): ApiResult<List<StorageItem>> = ApiResult.Error("", "")
    override suspend fun emptyTrash(): ApiResult<BatchResult> = ApiResult.Error("", "")
    override suspend fun getStarred(): ApiResult<List<StorageItem>> = ApiResult.Error("", "")
    override suspend fun getRecent(limit: Int): ApiResult<List<StorageItem>> = ApiResult.Error("", "")
    override suspend fun search(
        query: String,
        limit: Int,
        offset: Int,
    ): ApiResult<PaginatedResponse<StorageItem>> = ApiResult.Error("", "")

    override suspend fun batchDelete(
        itemIds: List<String>,
        permanent: Boolean,
    ): ApiResult<BatchResult> = ApiResult.Error("", "")

    override suspend fun batchMove(itemIds: List<String>, destinationId: String?): ApiResult<BatchResult> =
        ApiResult.Error("", "")

    override suspend fun batchCopy(itemIds: List<String>, destinationId: String?): ApiResult<BatchResult> =
        ApiResult.Error("", "")

    override suspend fun batchStar(itemIds: List<String>, starred: Boolean): ApiResult<BatchResult> =
        ApiResult.Error("", "")

    override suspend fun uploadFile(
        fileName: String,
        fileData: ByteArray,
        mimeType: String,
        parentId: String?,
        onProgress: (Float) -> Unit,
    ): ApiResult<StorageItem> = ApiResult.Error("", "")

    override suspend fun uploadFolder(
        files: List<FolderUploadFile>,
        parentId: String?,
        onProgress: (Float) -> Unit,
    ): ApiResult<FolderUploadResult> = ApiResult.Error("", "")

    override suspend fun initChunkedUpload(
        fileName: String,
        totalSize: Long,
        mimeType: String?,
        parentId: String?,
        chunkSize: Long,
    ): ApiResult<ChunkedUploadInit> = ApiResult.Error("", "")

    override suspend fun uploadChunk(
        uploadId: String,
        chunkIndex: Int,
        chunkData: ByteArray,
    ): ApiResult<ChunkedUploadStatus> = ApiResult.Error("", "")

    override suspend fun getUploadStatus(uploadId: String): ApiResult<ChunkedUploadStatus> = ApiResult.Error("", "")
    override suspend fun completeChunkedUpload(uploadId: String): ApiResult<StorageItem> = ApiResult.Error("", "")
    override suspend fun cancelChunkedUpload(uploadId: String): ApiResult<Unit> = ApiResult.Error("", "")
    override suspend fun downloadFile(itemId: String): ApiResult<ByteArray> = ApiResult.Error("", "")
    override fun getDownloadUrl(itemId: String): String = ""
    override fun getThumbnailUrl(itemId: String, size: String): String = ""
    override fun getPreviewUrl(itemId: String): String = ""
}

class UploadManagerDestinationTest {

    @Test
    fun `setUploadDestination and getCurrentDestinationFolderId update destination`() =
        ViewModelTestBase.withMainDispatcher {
            val manager = UploadManager(StubStorageRepository())
            assertEquals(null, manager.getCurrentDestinationFolderId())
            manager.setUploadDestination("folder-1")
            assertEquals("folder-1", manager.getCurrentDestinationFolderId())
            manager.setUploadDestination(null)
            assertEquals(null, manager.getCurrentDestinationFolderId())
        }
}

class UploadManagerMinimizedTest {

    @Test
    fun `setMinimized updates isMinimized state`() = ViewModelTestBase.runTestWithMain {
        val manager = UploadManager(StubStorageRepository())
        assertFalse(manager.isMinimized.first())
        manager.setMinimized(true)
        assertTrue(manager.isMinimized.first())
        manager.setMinimized(false)
        assertFalse(manager.isMinimized.first())
    }
}
