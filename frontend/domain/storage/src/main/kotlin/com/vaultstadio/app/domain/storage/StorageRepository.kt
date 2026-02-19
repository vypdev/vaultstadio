/**
 * Storage repository interface.
 */

package com.vaultstadio.app.domain.storage

import com.vaultstadio.app.domain.result.Result
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
    ): Result<PaginatedResponse<StorageItem>>
    suspend fun getItem(itemId: String): Result<StorageItem>
    suspend fun createFolder(name: String, parentId: String? = null): Result<StorageItem>
    suspend fun getBreadcrumbs(itemId: String): Result<List<Breadcrumb>>
    suspend fun renameItem(itemId: String, newName: String): Result<StorageItem>
    suspend fun moveItem(itemId: String, destinationId: String?, newName: String? = null): Result<StorageItem>
    suspend fun copyItem(itemId: String, destinationId: String?, newName: String? = null): Result<StorageItem>
    suspend fun toggleStar(itemId: String): Result<StorageItem>
    suspend fun trashItem(itemId: String): Result<StorageItem>
    suspend fun deleteItemPermanently(itemId: String): Result<Unit>
    suspend fun restoreItem(itemId: String): Result<StorageItem>
    suspend fun getTrash(): Result<List<StorageItem>>
    suspend fun emptyTrash(): Result<BatchResult>
    suspend fun getStarred(): Result<List<StorageItem>>
    suspend fun getRecent(limit: Int = 20): Result<List<StorageItem>>
    suspend fun search(query: String, limit: Int = 50, offset: Int = 0): Result<PaginatedResponse<StorageItem>>
    suspend fun batchDelete(itemIds: List<String>, permanent: Boolean = false): Result<BatchResult>
    suspend fun batchMove(itemIds: List<String>, destinationId: String?): Result<BatchResult>
    suspend fun batchCopy(itemIds: List<String>, destinationId: String?): Result<BatchResult>
    suspend fun batchStar(itemIds: List<String>, starred: Boolean): Result<BatchResult>
    suspend fun uploadFile(
        fileName: String,
        fileData: ByteArray,
        mimeType: String,
        parentId: String? = null,
        onProgress: (Float) -> Unit = {},
    ): Result<StorageItem>
    suspend fun uploadFolder(
        files: List<FolderUploadFile>,
        parentId: String?,
        onProgress: (Float) -> Unit = {},
    ): Result<FolderUploadResult>
    suspend fun initChunkedUpload(
        fileName: String,
        totalSize: Long,
        mimeType: String? = null,
        parentId: String? = null,
        chunkSize: Long = 10 * 1024 * 1024,
    ): Result<ChunkedUploadInit>
    suspend fun uploadChunk(uploadId: String, chunkIndex: Int, chunkData: ByteArray): Result<ChunkedUploadStatus>
    suspend fun getUploadStatus(uploadId: String): Result<ChunkedUploadStatus>
    suspend fun completeChunkedUpload(uploadId: String): Result<StorageItem>
    suspend fun cancelChunkedUpload(uploadId: String): Result<Unit>
    suspend fun downloadFile(itemId: String): Result<ByteArray>
    fun getDownloadUrl(itemId: String): String
    fun getThumbnailUrl(itemId: String, size: String = "medium"): String
    fun getPreviewUrl(itemId: String): String
}
