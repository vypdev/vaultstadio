package com.vaultstadio.app.feature.upload

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

/** Stub repository used when testing UploadManager state only (no uploads). */
internal open class StubStorageRepository : StorageRepository {
    override suspend fun getItems(
        folderId: String?,
        sortBy: SortField,
        sortOrder: SortOrder,
        limit: Int,
        offset: Int,
    ): Result<PaginatedResponse<StorageItem>> = Result.Error("", "")

    override suspend fun getItem(itemId: String): Result<StorageItem> = Result.Error("", "")
    override suspend fun createFolder(name: String, parentId: String?): Result<StorageItem> =
        Result.Error("", "")

    override suspend fun getBreadcrumbs(itemId: String): Result<List<Breadcrumb>> = Result.Error("", "")
    override suspend fun renameItem(itemId: String, newName: String): Result<StorageItem> = Result.Error("", "")
    override suspend fun moveItem(
        itemId: String,
        destinationId: String?,
        newName: String?,
    ): Result<StorageItem> = Result.Error("", "")

    override suspend fun copyItem(
        itemId: String,
        destinationId: String?,
        newName: String?,
    ): Result<StorageItem> = Result.Error("", "")

    override suspend fun toggleStar(itemId: String): Result<StorageItem> = Result.Error("", "")
    override suspend fun trashItem(itemId: String): Result<StorageItem> = Result.Error("", "")
    override suspend fun deleteItemPermanently(itemId: String): Result<Unit> = Result.Error("", "")
    override suspend fun restoreItem(itemId: String): Result<StorageItem> = Result.Error("", "")
    override suspend fun getTrash(): Result<List<StorageItem>> = Result.Error("", "")
    override suspend fun emptyTrash(): Result<BatchResult> = Result.Error("", "")
    override suspend fun getStarred(): Result<List<StorageItem>> = Result.Error("", "")
    override suspend fun getRecent(limit: Int): Result<List<StorageItem>> = Result.Error("", "")
    override suspend fun search(
        query: String,
        limit: Int,
        offset: Int,
    ): Result<PaginatedResponse<StorageItem>> = Result.Error("", "")

    override suspend fun batchDelete(
        itemIds: List<String>,
        permanent: Boolean,
    ): Result<BatchResult> = Result.Error("", "")

    override suspend fun batchMove(itemIds: List<String>, destinationId: String?): Result<BatchResult> =
        Result.Error("", "")

    override suspend fun batchCopy(itemIds: List<String>, destinationId: String?): Result<BatchResult> =
        Result.Error("", "")

    override suspend fun batchStar(itemIds: List<String>, starred: Boolean): Result<BatchResult> =
        Result.Error("", "")

    override suspend fun uploadFile(
        fileName: String,
        fileData: ByteArray,
        mimeType: String,
        parentId: String?,
        onProgress: (Float) -> Unit,
    ): Result<StorageItem> = Result.Error("", "")

    override suspend fun uploadFolder(
        files: List<FolderUploadFile>,
        parentId: String?,
        onProgress: (Float) -> Unit,
    ): Result<FolderUploadResult> = Result.Error("", "")

    override suspend fun initChunkedUpload(
        fileName: String,
        totalSize: Long,
        mimeType: String?,
        parentId: String?,
        chunkSize: Long,
    ): Result<ChunkedUploadInit> = Result.Error("", "")

    override suspend fun uploadChunk(
        uploadId: String,
        chunkIndex: Int,
        chunkData: ByteArray,
    ): Result<ChunkedUploadStatus> = Result.Error("", "")

    override suspend fun getUploadStatus(uploadId: String): Result<ChunkedUploadStatus> = Result.Error("", "")
    override suspend fun completeChunkedUpload(uploadId: String): Result<StorageItem> = Result.Error("", "")
    override suspend fun cancelChunkedUpload(uploadId: String): Result<Unit> = Result.Error("", "")
    override suspend fun downloadFile(itemId: String): Result<ByteArray> = Result.Error("", "")
    override fun getDownloadUrl(itemId: String): String = ""
    override fun getThumbnailUrl(itemId: String, size: String): String = ""
    override fun getPreviewUrl(itemId: String): String = ""
}
