package com.vaultstadio.core.domain.service

import com.vaultstadio.domain.storage.model.StorageItem
import java.io.InputStream

/**
 * Input for creating a folder.
 */
data class CreateFolderInput(
    val name: String,
    val parentId: String? = null,
    val ownerId: String,
)

/**
 * Input for uploading a file.
 */
data class UploadFileInput(
    val name: String,
    val parentId: String? = null,
    val ownerId: String,
    val mimeType: String?,
    val size: Long,
    val inputStream: InputStream,
)

/**
 * Result of get or create folder operation.
 */
data class GetOrCreateFolderResult(
    val folder: StorageItem,
    val created: Boolean,
)

/**
 * Input for moving an item.
 */
data class MoveItemInput(
    val itemId: String,
    val newParentId: String?,
    val newName: String? = null,
    val userId: String,
)

/**
 * Input for copying an item.
 */
data class CopyItemInput(
    val itemId: String,
    val destinationParentId: String?,
    val newName: String? = null,
    val userId: String,
)

