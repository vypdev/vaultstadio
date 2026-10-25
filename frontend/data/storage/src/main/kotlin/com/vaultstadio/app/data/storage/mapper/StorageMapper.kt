/**
 * Storage Mappers – DTO to domain (storage) model.
 */

package com.vaultstadio.app.data.storage.mapper

import com.vaultstadio.app.data.network.dto.common.PaginatedResponseDTO
import com.vaultstadio.app.data.storage.dto.BatchErrorDTO
import com.vaultstadio.app.data.storage.dto.BatchResultDTO
import com.vaultstadio.app.data.storage.dto.BreadcrumbDTO
import com.vaultstadio.app.data.storage.dto.ChunkedUploadInitDTO
import com.vaultstadio.app.data.storage.dto.ChunkedUploadStatusDTO
import com.vaultstadio.app.data.storage.dto.FolderUploadErrorDTO
import com.vaultstadio.app.data.storage.dto.FolderUploadResultDTO
import com.vaultstadio.app.data.storage.dto.StorageItemDTO
import com.vaultstadio.app.domain.storage.model.BatchError
import com.vaultstadio.app.domain.storage.model.BatchResult
import com.vaultstadio.app.domain.storage.model.Breadcrumb
import com.vaultstadio.app.domain.storage.model.ChunkedUploadInit
import com.vaultstadio.app.domain.storage.model.ChunkedUploadStatus
import com.vaultstadio.app.domain.storage.model.FolderUploadError
import com.vaultstadio.app.domain.storage.model.FolderUploadResult
import com.vaultstadio.app.domain.storage.model.ItemType
import com.vaultstadio.app.domain.storage.model.PaginatedResponse
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.domain.storage.model.Visibility

fun StorageItemDTO.toDomain(): StorageItem = StorageItem(
    id = id,
    name = name,
    path = path,
    type = try {
        ItemType.valueOf(type.uppercase())
    } catch (e: IllegalArgumentException) {
        ItemType.FILE
    },
    parentId = parentId,
    size = size,
    mimeType = mimeType,
    visibility = try {
        Visibility.valueOf(visibility.uppercase())
    } catch (e: IllegalArgumentException) {
        Visibility.PRIVATE
    },
    isStarred = isStarred,
    isTrashed = isTrashed,
    createdAt = createdAt,
    updatedAt = updatedAt,
    metadata = metadata,
)

fun PaginatedResponseDTO<StorageItemDTO>.toDomain(): PaginatedResponse<StorageItem> = PaginatedResponse(
    items = items.map { it.toDomain() },
    total = total,
    page = page,
    pageSize = pageSize,
    totalPages = totalPages,
    hasMore = hasMore,
)

fun List<StorageItemDTO>.toDomainList(): List<StorageItem> = map { it.toDomain() }

fun BreadcrumbDTO.toDomain(): Breadcrumb = Breadcrumb(
    id = id,
    name = name,
    path = path,
)

fun List<BreadcrumbDTO>.toBreadcrumbList(): List<Breadcrumb> = map { it.toDomain() }

fun BatchResultDTO.toDomain(): BatchResult = BatchResult(
    successful = successful,
    failed = failed,
    errors = errors.map { it.toDomain() },
)

fun BatchErrorDTO.toDomain(): BatchError = BatchError(
    itemId = itemId,
    error = error,
)

fun ChunkedUploadInitDTO.toDomain(): ChunkedUploadInit = ChunkedUploadInit(
    uploadId = uploadId,
    chunkSize = chunkSize,
    totalChunks = totalChunks,
)

fun ChunkedUploadStatusDTO.toDomain(): ChunkedUploadStatus = ChunkedUploadStatus(
    uploadId = uploadId,
    fileName = fileName,
    totalSize = totalSize,
    uploadedBytes = uploadedBytes,
    progress = progress,
    receivedChunks = receivedChunks,
    missingChunks = missingChunks,
    isComplete = isComplete,
)

fun FolderUploadResultDTO.toDomain(): FolderUploadResult = FolderUploadResult(
    uploadedFiles = uploadedFiles,
    createdFolders = createdFolders,
    errors = errors.map { it.toDomain() },
)

fun FolderUploadErrorDTO.toDomain(): FolderUploadError = FolderUploadError(
    path = path,
    error = error,
)

fun StorageItem.toBreadcrumb(): Breadcrumb = Breadcrumb(
    id = id,
    name = name,
    path = path,
)
