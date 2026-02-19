/**
 * Storage Domain Models
 */

package com.vaultstadio.app.domain.model

import kotlinx.datetime.Instant

/**
 * Storage item type.
 */
enum class ItemType {
    FILE,
    FOLDER,
}

/**
 * Item visibility.
 */
enum class Visibility {
    PRIVATE,
    SHARED,
    PUBLIC,
}

/**
 * Storage item domain model.
 */
data class StorageItem(
    val id: String,
    val name: String,
    val path: String,
    val type: ItemType,
    val parentId: String?,
    val size: Long,
    val mimeType: String?,
    val visibility: Visibility,
    val isStarred: Boolean,
    val isTrashed: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val metadata: Map<String, String>? = null,
) {
    val isFolder: Boolean get() = type == ItemType.FOLDER
    val isFile: Boolean get() = type == ItemType.FILE
    val extension: String?
        get() = if (isFile) name.substringAfterLast('.', "").takeIf { it.isNotEmpty() } else null
}

/**
 * Breadcrumb for navigation.
 */
data class Breadcrumb(
    val id: String?,
    val name: String,
    val path: String,
)

/**
 * Paginated response wrapper.
 */
data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val hasMore: Boolean,
)

/**
 * Sort options.
 */
enum class SortField {
    NAME,
    SIZE,
    CREATED_AT,
    UPDATED_AT,
    TYPE,
}

enum class SortOrder {
    ASC,
    DESC,
}

/**
 * Result of a batch operation.
 */
data class BatchResult(
    val successful: Int,
    val failed: Int,
    val errors: List<BatchError> = emptyList(),
)

data class BatchError(
    val itemId: String,
    val error: String,
)

/**
 * Chunked upload models.
 */
data class ChunkedUploadInit(
    val uploadId: String,
    val chunkSize: Long,
    val totalChunks: Int,
)

data class ChunkedUploadStatus(
    val uploadId: String,
    val fileName: String,
    val totalSize: Long,
    val uploadedBytes: Long,
    val progress: Float,
    val receivedChunks: List<Int>,
    val missingChunks: List<Int>,
    val isComplete: Boolean,
)

/**
 * Folder upload models.
 */
data class FolderUploadFile(
    val name: String,
    val relativePath: String,
    val mimeType: String,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as FolderUploadFile
        return name == other.name && relativePath == other.relativePath
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + relativePath.hashCode()
        return result
    }
}

data class FolderUploadResult(
    val uploadedFiles: Int,
    val createdFolders: Int,
    val errors: List<FolderUploadError> = emptyList(),
)

data class FolderUploadError(
    val path: String,
    val error: String,
)
