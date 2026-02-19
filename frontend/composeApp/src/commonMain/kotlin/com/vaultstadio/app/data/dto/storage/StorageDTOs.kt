/**
 * Storage Data Transfer Objects
 */

package com.vaultstadio.app.data.dto.storage

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class StorageItemDTO(
    val id: String,
    val name: String,
    val path: String,
    val type: String,
    val parentId: String?,
    val size: Long,
    val mimeType: String?,
    val visibility: String,
    val isStarred: Boolean,
    val isTrashed: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val metadata: Map<String, String>? = null,
)

@Serializable
data class CreateFolderRequestDTO(
    val name: String,
    val parentId: String? = null,
)

@Serializable
data class RenameRequestDTO(
    val name: String,
)

@Serializable
data class MoveRequestDTO(
    val destinationId: String?,
    val newName: String? = null,
)

@Serializable
data class CopyRequestDTO(
    val destinationId: String?,
    val newName: String? = null,
)

@Serializable
data class BatchDeleteRequestDTO(
    val itemIds: List<String>,
    val permanent: Boolean = false,
)

@Serializable
data class BatchMoveRequestDTO(
    val itemIds: List<String>,
    val destinationId: String?,
)

@Serializable
data class BatchCopyRequestDTO(
    val itemIds: List<String>,
    val destinationId: String?,
)

@Serializable
data class BatchStarRequestDTO(
    val itemIds: List<String>,
    val starred: Boolean,
)

@Serializable
data class BatchResultDTO(
    val successful: Int,
    val failed: Int,
    val errors: List<BatchErrorDTO> = emptyList(),
)

@Serializable
data class BatchErrorDTO(
    val itemId: String,
    val error: String,
)

@Serializable
data class ChunkedUploadInitRequestDTO(
    val fileName: String,
    val totalSize: Long,
    val mimeType: String? = null,
    val parentId: String? = null,
    val chunkSize: Long = 10 * 1024 * 1024,
)

@Serializable
data class ChunkedUploadInitDTO(
    val uploadId: String,
    val chunkSize: Long,
    val totalChunks: Int,
)

@Serializable
data class ChunkedUploadStatusDTO(
    val uploadId: String,
    val fileName: String,
    val totalSize: Long,
    val uploadedBytes: Long,
    val progress: Float,
    val receivedChunks: List<Int>,
    val missingChunks: List<Int>,
    val isComplete: Boolean,
)

@Serializable
data class FolderUploadResultDTO(
    val uploadedFiles: Int,
    val createdFolders: Int,
    val errors: List<FolderUploadErrorDTO> = emptyList(),
)

@Serializable
data class FolderUploadErrorDTO(
    val path: String,
    val error: String,
)

@Serializable
data class BreadcrumbDTO(
    val id: String?,
    val name: String,
    val path: String,
)
