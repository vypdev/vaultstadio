/**
 * Storage Repository
 */

package com.vaultstadio.app.data.repository

import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.service.StorageService
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
import org.koin.core.annotation.Single

/**
 * Repository interface for storage operations.
 */
interface StorageRepository {
    suspend fun getItems(
        folderId: String? = null,
        sortBy: SortField = SortField.NAME,
        sortOrder: SortOrder = SortOrder.ASC,
        limit: Int = 100,
        offset: Int = 0,
    ): ApiResult<PaginatedResponse<StorageItem>>
    suspend fun getItem(itemId: String): ApiResult<StorageItem>
    suspend fun createFolder(name: String, parentId: String? = null): ApiResult<StorageItem>
    suspend fun getBreadcrumbs(itemId: String): ApiResult<List<Breadcrumb>>
    suspend fun renameItem(itemId: String, newName: String): ApiResult<StorageItem>
    suspend fun moveItem(itemId: String, destinationId: String?, newName: String? = null): ApiResult<StorageItem>
    suspend fun copyItem(itemId: String, destinationId: String?, newName: String? = null): ApiResult<StorageItem>
    suspend fun toggleStar(itemId: String): ApiResult<StorageItem>
    suspend fun trashItem(itemId: String): ApiResult<StorageItem>
    suspend fun deleteItemPermanently(itemId: String): ApiResult<Unit>
    suspend fun restoreItem(itemId: String): ApiResult<StorageItem>
    suspend fun getTrash(): ApiResult<List<StorageItem>>
    suspend fun emptyTrash(): ApiResult<BatchResult>
    suspend fun getStarred(): ApiResult<List<StorageItem>>
    suspend fun getRecent(limit: Int = 20): ApiResult<List<StorageItem>>
    suspend fun search(query: String, limit: Int = 50, offset: Int = 0): ApiResult<PaginatedResponse<StorageItem>>
    suspend fun batchDelete(itemIds: List<String>, permanent: Boolean = false): ApiResult<BatchResult>
    suspend fun batchMove(itemIds: List<String>, destinationId: String?): ApiResult<BatchResult>
    suspend fun batchCopy(itemIds: List<String>, destinationId: String?): ApiResult<BatchResult>
    suspend fun batchStar(itemIds: List<String>, starred: Boolean): ApiResult<BatchResult>
    suspend fun uploadFile(
        fileName: String,
        fileData: ByteArray,
        mimeType: String,
        parentId: String? = null,
        onProgress: (Float) -> Unit = {},
    ): ApiResult<StorageItem>
    suspend fun uploadFolder(
        files: List<FolderUploadFile>,
        parentId: String?,
        onProgress: (Float) -> Unit = {},
    ): ApiResult<FolderUploadResult>
    suspend fun initChunkedUpload(
        fileName: String,
        totalSize: Long,
        mimeType: String? = null,
        parentId: String? = null,
        chunkSize: Long = 10 * 1024 * 1024,
    ): ApiResult<ChunkedUploadInit>
    suspend fun uploadChunk(uploadId: String, chunkIndex: Int, chunkData: ByteArray): ApiResult<ChunkedUploadStatus>
    suspend fun getUploadStatus(uploadId: String): ApiResult<ChunkedUploadStatus>
    suspend fun completeChunkedUpload(uploadId: String): ApiResult<StorageItem>
    suspend fun cancelChunkedUpload(uploadId: String): ApiResult<Unit>
    suspend fun downloadFile(itemId: String): ApiResult<ByteArray>
    fun getDownloadUrl(itemId: String): String
    fun getThumbnailUrl(itemId: String, size: String = "medium"): String
    fun getPreviewUrl(itemId: String): String
}

@Single(binds = [StorageRepository::class])
class StorageRepositoryImpl(
    private val storageService: StorageService,
    private val config: ApiClientConfig,
    private val tokenStorage: TokenStorage,
) : StorageRepository {

    override suspend fun getItems(
        folderId: String?,
        sortBy: SortField,
        sortOrder: SortOrder,
        limit: Int,
        offset: Int,
    ): ApiResult<PaginatedResponse<StorageItem>> =
        storageService.listFolder(folderId, sortBy, sortOrder, limit, offset)

    override suspend fun getItem(itemId: String): ApiResult<StorageItem> =
        storageService.getItem(itemId)

    override suspend fun createFolder(name: String, parentId: String?): ApiResult<StorageItem> =
        storageService.createFolder(name, parentId)

    override suspend fun getBreadcrumbs(itemId: String): ApiResult<List<Breadcrumb>> =
        storageService.getBreadcrumbs(itemId)

    override suspend fun renameItem(itemId: String, newName: String): ApiResult<StorageItem> =
        storageService.renameItem(itemId, newName)

    override suspend fun moveItem(itemId: String, destinationId: String?, newName: String?): ApiResult<StorageItem> =
        storageService.moveItem(itemId, destinationId, newName)

    override suspend fun copyItem(itemId: String, destinationId: String?, newName: String?): ApiResult<StorageItem> =
        storageService.copyItem(itemId, destinationId, newName)

    override suspend fun toggleStar(itemId: String): ApiResult<StorageItem> =
        storageService.toggleStar(itemId)

    override suspend fun trashItem(itemId: String): ApiResult<StorageItem> =
        storageService.trashItem(itemId)

    override suspend fun deleteItemPermanently(itemId: String): ApiResult<Unit> =
        storageService.deleteItemPermanently(itemId)

    override suspend fun restoreItem(itemId: String): ApiResult<StorageItem> =
        storageService.restoreItem(itemId)

    override suspend fun getTrash(): ApiResult<List<StorageItem>> =
        storageService.getTrash()

    override suspend fun emptyTrash(): ApiResult<BatchResult> =
        storageService.emptyTrash()

    override suspend fun getStarred(): ApiResult<List<StorageItem>> =
        storageService.getStarred()

    override suspend fun getRecent(limit: Int): ApiResult<List<StorageItem>> =
        storageService.getRecent(limit)

    override suspend fun search(query: String, limit: Int, offset: Int): ApiResult<PaginatedResponse<StorageItem>> =
        storageService.search(query, limit, offset)

    override suspend fun batchDelete(itemIds: List<String>, permanent: Boolean): ApiResult<BatchResult> =
        storageService.batchDelete(itemIds, permanent)

    override suspend fun batchMove(itemIds: List<String>, destinationId: String?): ApiResult<BatchResult> =
        storageService.batchMove(itemIds, destinationId)

    override suspend fun batchCopy(itemIds: List<String>, destinationId: String?): ApiResult<BatchResult> =
        storageService.batchCopy(itemIds, destinationId)

    override suspend fun batchStar(itemIds: List<String>, starred: Boolean): ApiResult<BatchResult> =
        storageService.batchStar(itemIds, starred)

    override suspend fun uploadFile(
        fileName: String,
        fileData: ByteArray,
        mimeType: String,
        parentId: String?,
        onProgress: (Float) -> Unit,
    ): ApiResult<StorageItem> =
        storageService.uploadFile(fileName, fileData, mimeType, parentId, onProgress)

    override suspend fun uploadFolder(
        files: List<FolderUploadFile>,
        parentId: String?,
        onProgress: (Float) -> Unit,
    ): ApiResult<FolderUploadResult> =
        storageService.uploadFolder(files, parentId, { tokenStorage.getAccessToken() }, onProgress)

    override suspend fun initChunkedUpload(
        fileName: String,
        totalSize: Long,
        mimeType: String?,
        parentId: String?,
        chunkSize: Long,
    ): ApiResult<ChunkedUploadInit> =
        storageService.initChunkedUpload(fileName, totalSize, mimeType, parentId, chunkSize)

    override suspend fun uploadChunk(
        uploadId: String,
        chunkIndex: Int,
        chunkData: ByteArray,
    ): ApiResult<ChunkedUploadStatus> =
        storageService.uploadChunk(uploadId, chunkIndex, chunkData)

    override suspend fun getUploadStatus(uploadId: String): ApiResult<ChunkedUploadStatus> =
        storageService.getUploadStatus(uploadId)

    override suspend fun completeChunkedUpload(uploadId: String): ApiResult<StorageItem> =
        storageService.completeChunkedUpload(uploadId)

    override suspend fun cancelChunkedUpload(uploadId: String): ApiResult<Unit> =
        storageService.cancelChunkedUpload(uploadId)

    override suspend fun downloadFile(itemId: String): ApiResult<ByteArray> =
        storageService.downloadFile(itemId)

    override fun getDownloadUrl(itemId: String): String {
        val token = tokenStorage.getAccessToken()
        return "${config.baseUrl}/api/v1/storage/item/$itemId/download${if (token != null) "?token=$token" else ""}"
    }

    override fun getThumbnailUrl(itemId: String, size: String): String {
        val token = tokenStorage.getAccessToken()
        val auth = if (token != null) "&token=$token" else ""
        return "${config.baseUrl}/api/v1/storage/item/$itemId/thumbnail?size=$size$auth"
    }

    override fun getPreviewUrl(itemId: String): String {
        val token = tokenStorage.getAccessToken()
        return "${config.baseUrl}/api/v1/storage/item/$itemId/preview${if (token != null) "?token=$token" else ""}"
    }
}
