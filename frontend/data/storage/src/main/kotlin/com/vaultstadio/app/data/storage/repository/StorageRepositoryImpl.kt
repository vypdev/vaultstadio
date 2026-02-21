/**
 * Storage repository implementation – implements domain StorageRepository.
 */

package com.vaultstadio.app.data.storage.repository

import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.network.TokenStorage
import com.vaultstadio.app.data.network.mapper.toResult
import com.vaultstadio.app.data.storage.service.StorageService
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.model.BatchResult
import com.vaultstadio.app.domain.storage.model.Breadcrumb
import com.vaultstadio.app.domain.storage.model.ChunkedUploadInit
import com.vaultstadio.app.domain.storage.model.ChunkedUploadStatus
import com.vaultstadio.app.domain.storage.model.FolderUploadFile
import com.vaultstadio.app.domain.storage.model.FolderUploadResult
import com.vaultstadio.app.domain.storage.model.PaginatedResponse
import com.vaultstadio.app.domain.storage.model.SortField
import com.vaultstadio.app.domain.storage.model.SortOrder
import com.vaultstadio.app.domain.storage.model.StorageItem
import org.koin.core.annotation.Single

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
    ): Result<PaginatedResponse<StorageItem>> =
        storageService.listFolder(folderId, sortBy, sortOrder, limit, offset).toResult()

    override suspend fun getItem(itemId: String): Result<StorageItem> =
        storageService.getItem(itemId).toResult()

    override suspend fun createFolder(name: String, parentId: String?): Result<StorageItem> =
        storageService.createFolder(name, parentId).toResult()

    override suspend fun getBreadcrumbs(itemId: String): Result<List<Breadcrumb>> =
        storageService.getBreadcrumbs(itemId).toResult()

    override suspend fun renameItem(itemId: String, newName: String): Result<StorageItem> =
        storageService.renameItem(itemId, newName).toResult()

    override suspend fun moveItem(itemId: String, destinationId: String?, newName: String?): Result<StorageItem> =
        storageService.moveItem(itemId, destinationId, newName).toResult()

    override suspend fun copyItem(itemId: String, destinationId: String?, newName: String?): Result<StorageItem> =
        storageService.copyItem(itemId, destinationId, newName).toResult()

    override suspend fun toggleStar(itemId: String): Result<StorageItem> =
        storageService.toggleStar(itemId).toResult()

    override suspend fun trashItem(itemId: String): Result<StorageItem> =
        storageService.trashItem(itemId).toResult()

    override suspend fun deleteItemPermanently(itemId: String): Result<Unit> =
        storageService.deleteItemPermanently(itemId).toResult()

    override suspend fun restoreItem(itemId: String): Result<StorageItem> =
        storageService.restoreItem(itemId).toResult()

    override suspend fun getTrash(): Result<List<StorageItem>> =
        storageService.getTrash().toResult()

    override suspend fun emptyTrash(): Result<BatchResult> =
        storageService.emptyTrash().toResult()

    override suspend fun getStarred(): Result<List<StorageItem>> =
        storageService.getStarred().toResult()

    override suspend fun getRecent(limit: Int): Result<List<StorageItem>> =
        storageService.getRecent(limit).toResult()

    override suspend fun search(query: String, limit: Int, offset: Int): Result<PaginatedResponse<StorageItem>> =
        storageService.search(query, limit, offset).toResult()

    override suspend fun batchDelete(itemIds: List<String>, permanent: Boolean): Result<BatchResult> =
        storageService.batchDelete(itemIds, permanent).toResult()

    override suspend fun batchMove(itemIds: List<String>, destinationId: String?): Result<BatchResult> =
        storageService.batchMove(itemIds, destinationId).toResult()

    override suspend fun batchCopy(itemIds: List<String>, destinationId: String?): Result<BatchResult> =
        storageService.batchCopy(itemIds, destinationId).toResult()

    override suspend fun batchStar(itemIds: List<String>, starred: Boolean): Result<BatchResult> =
        storageService.batchStar(itemIds, starred).toResult()

    override suspend fun uploadFile(
        fileName: String,
        fileData: ByteArray,
        mimeType: String,
        parentId: String?,
        onProgress: (Float) -> Unit,
    ): Result<StorageItem> =
        storageService.uploadFile(fileName, fileData, mimeType, parentId, onProgress).toResult()

    override suspend fun uploadFolder(
        files: List<FolderUploadFile>,
        parentId: String?,
        onProgress: (Float) -> Unit,
    ): Result<FolderUploadResult> =
        storageService.uploadFolder(files, parentId, { tokenStorage.getAccessToken() }, onProgress).toResult()

    override suspend fun initChunkedUpload(
        fileName: String,
        totalSize: Long,
        mimeType: String?,
        parentId: String?,
        chunkSize: Long,
    ): Result<ChunkedUploadInit> =
        storageService.initChunkedUpload(fileName, totalSize, mimeType, parentId, chunkSize).toResult()

    override suspend fun uploadChunk(
        uploadId: String,
        chunkIndex: Int,
        chunkData: ByteArray,
    ): Result<ChunkedUploadStatus> =
        storageService.uploadChunk(uploadId, chunkIndex, chunkData).toResult()

    override suspend fun getUploadStatus(uploadId: String): Result<ChunkedUploadStatus> =
        storageService.getUploadStatus(uploadId).toResult()

    override suspend fun completeChunkedUpload(uploadId: String): Result<StorageItem> =
        storageService.completeChunkedUpload(uploadId).toResult()

    override suspend fun cancelChunkedUpload(uploadId: String): Result<Unit> =
        storageService.cancelChunkedUpload(uploadId).toResult()

    override suspend fun downloadFile(itemId: String): Result<ByteArray> =
        storageService.downloadFile(itemId).toResult()

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
