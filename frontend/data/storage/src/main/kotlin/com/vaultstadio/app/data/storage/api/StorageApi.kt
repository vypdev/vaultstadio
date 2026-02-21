/**
 * Storage API – storage-related HTTP calls.
 */

package com.vaultstadio.app.data.storage.api

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.BaseApi
import com.vaultstadio.app.data.network.dto.common.ApiResponseDTO
import com.vaultstadio.app.data.network.dto.common.PaginatedResponseDTO
import com.vaultstadio.app.data.storage.dto.BatchCopyRequestDTO
import com.vaultstadio.app.data.storage.dto.BatchDeleteRequestDTO
import com.vaultstadio.app.data.storage.dto.BatchMoveRequestDTO
import com.vaultstadio.app.data.storage.dto.BatchResultDTO
import com.vaultstadio.app.data.storage.dto.BatchStarRequestDTO
import com.vaultstadio.app.data.storage.dto.ChunkedUploadInitDTO
import com.vaultstadio.app.data.storage.dto.ChunkedUploadInitRequestDTO
import com.vaultstadio.app.data.storage.dto.ChunkedUploadStatusDTO
import com.vaultstadio.app.data.storage.dto.CopyRequestDTO
import com.vaultstadio.app.data.storage.dto.CreateFolderRequestDTO
import com.vaultstadio.app.data.storage.dto.FolderUploadResultDTO
import com.vaultstadio.app.data.storage.dto.MoveRequestDTO
import com.vaultstadio.app.data.storage.dto.RenameRequestDTO
import com.vaultstadio.app.data.storage.dto.StorageItemDTO
import com.vaultstadio.app.domain.storage.model.FolderUploadFile
import com.vaultstadio.app.domain.storage.model.SortField
import com.vaultstadio.app.domain.storage.model.SortOrder
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import org.koin.core.annotation.Single

@Single
class StorageApi(client: HttpClient) : BaseApi(client) {

    suspend fun listFolder(
        folderId: String? = null,
        sortBy: SortField = SortField.NAME,
        sortOrder: SortOrder = SortOrder.ASC,
        limit: Int = 100,
        offset: Int = 0,
    ): ApiResult<PaginatedResponseDTO<StorageItemDTO>> {
        val path = if (folderId != null) {
            "/api/v1/storage/folder/$folderId"
        } else {
            "/api/v1/storage/folder"
        }
        return get(
            path,
            mapOf(
                "sortBy" to sortBy.name.lowercase(),
                "sortOrder" to sortOrder.name.lowercase(),
                "limit" to limit.toString(),
                "offset" to offset.toString(),
            ),
        )
    }

    suspend fun getItem(itemId: String): ApiResult<StorageItemDTO> =
        get("/api/v1/storage/item/$itemId")

    suspend fun createFolder(request: CreateFolderRequestDTO): ApiResult<StorageItemDTO> =
        post("/api/v1/storage/folder", request)

    suspend fun renameItem(itemId: String, request: RenameRequestDTO): ApiResult<StorageItemDTO> =
        patch("/api/v1/storage/item/$itemId/rename", request)

    suspend fun moveItem(itemId: String, request: MoveRequestDTO): ApiResult<StorageItemDTO> =
        post("/api/v1/storage/item/$itemId/move", request)

    suspend fun copyItem(itemId: String, request: CopyRequestDTO): ApiResult<StorageItemDTO> =
        post("/api/v1/storage/item/$itemId/copy", request)

    suspend fun toggleStar(itemId: String): ApiResult<StorageItemDTO> =
        postNoBody("/api/v1/storage/item/$itemId/star")

    suspend fun trashItem(itemId: String): ApiResult<StorageItemDTO> =
        delete("/api/v1/storage/item/$itemId")

    suspend fun deleteItemPermanently(itemId: String): ApiResult<Unit> =
        deleteWithParams("/api/v1/storage/item/$itemId", mapOf("permanent" to "true"))

    suspend fun restoreItem(itemId: String): ApiResult<StorageItemDTO> =
        postNoBody("/api/v1/storage/item/$itemId/restore")

    suspend fun getTrash(): ApiResult<List<StorageItemDTO>> =
        get("/api/v1/storage/trash")

    suspend fun getStarred(): ApiResult<List<StorageItemDTO>> =
        get("/api/v1/storage/starred")

    suspend fun getRecent(limit: Int = 20): ApiResult<List<StorageItemDTO>> =
        get("/api/v1/storage/recent", mapOf("limit" to limit.toString()))

    suspend fun getBreadcrumbs(itemId: String): ApiResult<List<StorageItemDTO>> =
        get("/api/v1/storage/item/$itemId/breadcrumbs")

    suspend fun search(
        query: String,
        limit: Int = 50,
        offset: Int = 0,
    ): ApiResult<PaginatedResponseDTO<StorageItemDTO>> =
        get(
            "/api/v1/search",
            mapOf(
                "q" to query,
                "limit" to limit.toString(),
                "offset" to offset.toString(),
            ),
        )

    suspend fun batchDelete(request: BatchDeleteRequestDTO): ApiResult<BatchResultDTO> =
        post("/api/v1/storage/batch/delete", request)

    suspend fun batchMove(request: BatchMoveRequestDTO): ApiResult<BatchResultDTO> =
        post("/api/v1/storage/batch/move", request)

    suspend fun batchCopy(request: BatchCopyRequestDTO): ApiResult<BatchResultDTO> =
        post("/api/v1/storage/batch/copy", request)

    suspend fun batchStar(request: BatchStarRequestDTO): ApiResult<BatchResultDTO> =
        post("/api/v1/storage/batch/star", request)

    suspend fun emptyTrash(): ApiResult<BatchResultDTO> =
        postNoBody("/api/v1/storage/batch/empty-trash")

    suspend fun initChunkedUpload(request: ChunkedUploadInitRequestDTO): ApiResult<ChunkedUploadInitDTO> =
        post("/api/v1/storage/upload/init", request)

    suspend fun uploadChunk(
        uploadId: String,
        chunkIndex: Int,
        chunkData: ByteArray,
    ): ApiResult<ChunkedUploadStatusDTO> {
        return try {
            val response = client.submitFormWithBinaryData(
                url = "/api/v1/storage/upload/$uploadId/chunk/$chunkIndex",
                formData = formData {
                    append(
                        "chunk",
                        chunkData,
                        Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"chunk_$chunkIndex\"")
                            append(HttpHeaders.ContentType, "application/octet-stream")
                        },
                    )
                },
            )

            if (response.status.isSuccess()) {
                val apiResponse = response.body<ApiResponseDTO<ChunkedUploadStatusDTO>>()
                val data = apiResponse.data
                if (apiResponse.success && data != null) {
                    ApiResult.Success(data)
                } else {
                    ApiResult.Error(
                        apiResponse.error?.code ?: "CHUNK_ERROR",
                        apiResponse.error?.message ?: "Chunk upload failed",
                    )
                }
            } else {
                ApiResult.Error("HTTP_${response.status.value}", response.status.description)
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e.message ?: "Chunk upload failed")
        }
    }

    suspend fun getUploadStatus(uploadId: String): ApiResult<ChunkedUploadStatusDTO> =
        get("/api/v1/storage/upload/$uploadId/status")

    suspend fun completeChunkedUpload(uploadId: String): ApiResult<StorageItemDTO> =
        postNoBody("/api/v1/storage/upload/$uploadId/complete")

    suspend fun cancelChunkedUpload(uploadId: String): ApiResult<Unit> =
        delete("/api/v1/storage/upload/$uploadId")

    suspend fun uploadFile(
        fileName: String,
        fileData: ByteArray,
        mimeType: String,
        parentId: String? = null,
        onProgress: (Float) -> Unit = {},
    ): ApiResult<StorageItemDTO> {
        return try {
            val response = client.submitFormWithBinaryData(
                url = "/api/v1/storage/upload",
                formData = formData {
                    append(
                        "file",
                        fileData,
                        Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            append(HttpHeaders.ContentType, mimeType)
                        },
                    )
                    parentId?.let {
                        append("parentId", it)
                    }
                },
            ) {
                onUpload { bytesSentTotal, contentLength ->
                    val total = contentLength ?: 0L
                    if (total > 0) {
                        onProgress(bytesSentTotal.toFloat() / total.toFloat())
                    }
                }
            }

            if (response.status.isSuccess()) {
                val apiResponse = response.body<ApiResponseDTO<StorageItemDTO>>()
                val data = apiResponse.data
                if (apiResponse.success && data != null) {
                    ApiResult.Success(data)
                } else {
                    ApiResult.Error(
                        apiResponse.error?.code ?: "UPLOAD_ERROR",
                        apiResponse.error?.message ?: "Upload failed",
                    )
                }
            } else {
                ApiResult.Error("HTTP_${response.status.value}", response.status.description)
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e.message ?: "Upload failed")
        }
    }

    suspend fun uploadFolder(
        files: List<FolderUploadFile>,
        parentId: String?,
        tokenProvider: () -> String?,
        onProgress: (Float) -> Unit = {},
    ): ApiResult<FolderUploadResultDTO> {
        return try {
            val response = client.submitFormWithBinaryData(
                url = "/api/v1/storage/upload-folder",
                formData = formData {
                    parentId?.let { append("parentId", it) }
                    files.forEachIndexed { index, file ->
                        append(
                            file.relativePath,
                            file.data,
                            Headers.build {
                                append(HttpHeaders.ContentType, file.mimeType)
                                append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                            },
                        )
                        onProgress((index + 1).toFloat() / files.size)
                    }
                },
            ) {
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider()}")
            }

            if (response.status.isSuccess()) {
                val apiResponse = response.body<ApiResponseDTO<FolderUploadResultDTO>>()
                val data = apiResponse.data
                if (apiResponse.success && data != null) {
                    ApiResult.Success(data)
                } else {
                    ApiResult.Error(
                        apiResponse.error?.code ?: "UPLOAD_ERROR",
                        apiResponse.error?.message ?: "Upload failed",
                    )
                }
            } else {
                ApiResult.Error("HTTP_ERROR", "Upload failed: ${response.status}")
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e.message ?: "Upload error")
        }
    }

    suspend fun downloadFile(itemId: String): ApiResult<ByteArray> {
        return try {
            val response = client.get("/api/v1/storage/item/$itemId/download")
            if (response.status.isSuccess()) {
                val bytes = response.body<ByteArray>()
                ApiResult.Success(bytes)
            } else {
                ApiResult.Error("HTTP_${response.status.value}", response.status.description)
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e.message ?: "Download failed")
        }
    }
}
