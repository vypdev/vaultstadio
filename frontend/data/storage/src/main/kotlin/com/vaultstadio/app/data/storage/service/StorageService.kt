/**
 * Storage Service – delegates to StorageApi and maps DTOs to domain.
 */

package com.vaultstadio.app.data.storage.service

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.storage.api.StorageApi
import com.vaultstadio.app.data.storage.dto.BatchCopyRequestDTO
import com.vaultstadio.app.data.storage.dto.BatchDeleteRequestDTO
import com.vaultstadio.app.data.storage.dto.BatchMoveRequestDTO
import com.vaultstadio.app.data.storage.dto.BatchStarRequestDTO
import com.vaultstadio.app.data.storage.dto.ChunkedUploadInitRequestDTO
import com.vaultstadio.app.data.storage.dto.CopyRequestDTO
import com.vaultstadio.app.data.storage.dto.CreateFolderRequestDTO
import com.vaultstadio.app.data.storage.dto.MoveRequestDTO
import com.vaultstadio.app.data.storage.dto.RenameRequestDTO
import com.vaultstadio.app.data.storage.mapper.toBreadcrumb
import com.vaultstadio.app.data.storage.mapper.toDomain
import com.vaultstadio.app.data.storage.mapper.toDomainList
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

@Single
class StorageService(private val storageApi: StorageApi) {

    suspend fun listFolder(
        folderId: String? = null,
        sortBy: SortField = SortField.NAME,
        sortOrder: SortOrder = SortOrder.ASC,
        limit: Int = 100,
        offset: Int = 0,
    ): ApiResult<PaginatedResponse<StorageItem>> =
        storageApi.listFolder(folderId, sortBy, sortOrder, limit, offset).map { it.toDomain() }

    suspend fun getItem(itemId: String): ApiResult<StorageItem> =
        storageApi.getItem(itemId).map { it.toDomain() }

    suspend fun createFolder(name: String, parentId: String?): ApiResult<StorageItem> =
        storageApi.createFolder(CreateFolderRequestDTO(name, parentId)).map { it.toDomain() }

    suspend fun renameItem(itemId: String, newName: String): ApiResult<StorageItem> =
        storageApi.renameItem(itemId, RenameRequestDTO(newName)).map { it.toDomain() }

    suspend fun moveItem(itemId: String, destinationId: String?, newName: String?): ApiResult<StorageItem> =
        storageApi.moveItem(itemId, MoveRequestDTO(destinationId, newName)).map { it.toDomain() }

    suspend fun copyItem(itemId: String, destinationId: String?, newName: String?): ApiResult<StorageItem> =
        storageApi.copyItem(itemId, CopyRequestDTO(destinationId, newName)).map { it.toDomain() }

    suspend fun toggleStar(itemId: String): ApiResult<StorageItem> =
        storageApi.toggleStar(itemId).map { it.toDomain() }

    suspend fun trashItem(itemId: String): ApiResult<StorageItem> =
        storageApi.trashItem(itemId).map { it.toDomain() }

    suspend fun deleteItemPermanently(itemId: String): ApiResult<Unit> =
        storageApi.deleteItemPermanently(itemId)

    suspend fun restoreItem(itemId: String): ApiResult<StorageItem> =
        storageApi.restoreItem(itemId).map { it.toDomain() }

    suspend fun getTrash(): ApiResult<List<StorageItem>> =
        storageApi.getTrash().map { it.toDomainList() }

    suspend fun getStarred(): ApiResult<List<StorageItem>> =
        storageApi.getStarred().map { it.toDomainList() }

    suspend fun getRecent(limit: Int = 20): ApiResult<List<StorageItem>> =
        storageApi.getRecent(limit).map { it.toDomainList() }

    suspend fun getBreadcrumbs(itemId: String): ApiResult<List<Breadcrumb>> =
        storageApi.getBreadcrumbs(itemId).map { items -> items.map { it.toDomain().toBreadcrumb() } }

    suspend fun search(query: String, limit: Int = 50, offset: Int = 0): ApiResult<PaginatedResponse<StorageItem>> =
        storageApi.search(query, limit, offset).map { it.toDomain() }

    suspend fun batchDelete(itemIds: List<String>, permanent: Boolean = false): ApiResult<BatchResult> =
        storageApi.batchDelete(BatchDeleteRequestDTO(itemIds, permanent)).map { it.toDomain() }

    suspend fun batchMove(itemIds: List<String>, destinationId: String?): ApiResult<BatchResult> =
        storageApi.batchMove(BatchMoveRequestDTO(itemIds, destinationId)).map { it.toDomain() }

    suspend fun batchCopy(itemIds: List<String>, destinationId: String?): ApiResult<BatchResult> =
        storageApi.batchCopy(BatchCopyRequestDTO(itemIds, destinationId)).map { it.toDomain() }

    suspend fun batchStar(itemIds: List<String>, starred: Boolean): ApiResult<BatchResult> =
        storageApi.batchStar(BatchStarRequestDTO(itemIds, starred)).map { it.toDomain() }

    suspend fun emptyTrash(): ApiResult<BatchResult> =
        storageApi.emptyTrash().map { it.toDomain() }

    suspend fun initChunkedUpload(
        fileName: String,
        totalSize: Long,
        mimeType: String?,
        parentId: String?,
        chunkSize: Long = 10 * 1024 * 1024,
    ): ApiResult<ChunkedUploadInit> =
        storageApi.initChunkedUpload(
            ChunkedUploadInitRequestDTO(fileName, totalSize, mimeType, parentId, chunkSize),
        ).map { it.toDomain() }

    suspend fun uploadChunk(uploadId: String, chunkIndex: Int, chunkData: ByteArray): ApiResult<ChunkedUploadStatus> =
        storageApi.uploadChunk(uploadId, chunkIndex, chunkData).map { it.toDomain() }

    suspend fun getUploadStatus(uploadId: String): ApiResult<ChunkedUploadStatus> =
        storageApi.getUploadStatus(uploadId).map { it.toDomain() }

    suspend fun completeChunkedUpload(uploadId: String): ApiResult<StorageItem> =
        storageApi.completeChunkedUpload(uploadId).map { it.toDomain() }

    suspend fun cancelChunkedUpload(uploadId: String): ApiResult<Unit> =
        storageApi.cancelChunkedUpload(uploadId)

    suspend fun uploadFile(
        fileName: String,
        fileData: ByteArray,
        mimeType: String,
        parentId: String?,
        onProgress: (Float) -> Unit = {},
    ): ApiResult<StorageItem> =
        storageApi.uploadFile(fileName, fileData, mimeType, parentId, onProgress).map { it.toDomain() }

    suspend fun uploadFolder(
        files: List<FolderUploadFile>,
        parentId: String?,
        tokenProvider: () -> String?,
        onProgress: (Float) -> Unit = {},
    ): ApiResult<FolderUploadResult> =
        storageApi.uploadFolder(files, parentId, tokenProvider, onProgress).map { it.toDomain() }

    suspend fun downloadFile(itemId: String): ApiResult<ByteArray> =
        storageApi.downloadFile(itemId)
}
