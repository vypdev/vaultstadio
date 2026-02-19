/**
 * VaultStadio Storage Service
 *
 * Core service for file and folder operations.
 * This is the main entry point for all storage operations.
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.event.FileEvent
import com.vaultstadio.core.domain.model.ItemType
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.model.StorageQuota
import com.vaultstadio.core.domain.repository.PagedResult
import com.vaultstadio.core.domain.repository.SortField
import com.vaultstadio.core.domain.repository.SortOrder
import com.vaultstadio.core.domain.repository.StorageItemQuery
import com.vaultstadio.core.domain.repository.StorageItemRepository
import com.vaultstadio.core.exception.StorageException
import com.vaultstadio.core.exception.ValidationException
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.InputStream

private val logger = KotlinLogging.logger {}

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

/**
 * Storage backend interface for actual file operations.
 */
interface StorageBackend {
    suspend fun store(inputStream: InputStream, size: Long, mimeType: String?): Either<StorageException, String>
    suspend fun retrieve(storageKey: String): Either<StorageException, InputStream>
    suspend fun delete(storageKey: String): Either<StorageException, Unit>
    suspend fun exists(storageKey: String): Either<StorageException, Boolean>
    suspend fun getSize(storageKey: String): Either<StorageException, Long>
    suspend fun copy(sourceKey: String): Either<StorageException, String>
    suspend fun getPresignedUrl(storageKey: String, expirationSeconds: Long): Either<StorageException, String?>

    /**
     * Checks if the storage backend is available and operational.
     * Used for health checks.
     */
    suspend fun isAvailable(): Either<StorageException, Boolean>
}

/**
 * Service for file and folder operations.
 */
class StorageService(
    private val storageItemRepository: StorageItemRepository,
    private val storageBackend: StorageBackend,
    private val eventBus: EventBus,
) {

    /**
     * Creates a new folder.
     */
    suspend fun createFolder(input: CreateFolderInput): Either<StorageException, StorageItem> =
        createFolderInternal(storageItemRepository, eventBus, input)

    /**
     * Gets an existing folder by name in the parent, or creates it if it doesn't exist.
     */
    suspend fun getOrCreateFolder(
        name: String,
        parentId: String?,
        ownerId: String,
    ): Either<StorageException, GetOrCreateFolderResult> =
        getOrCreateFolderInternal(storageItemRepository, eventBus, name, parentId, ownerId)

    /**
     * Uploads a new file.
     */
    suspend fun uploadFile(input: UploadFileInput): Either<StorageException, StorageItem> =
        uploadFileInternal(storageItemRepository, storageBackend, eventBus, input)

    /**
     * Gets an item by ID.
     */
    suspend fun getItem(itemId: String, userId: String): Either<StorageException, StorageItem> =
        getItemInternal(storageItemRepository, itemId, userId)

    /**
     * Lists folder contents with query parameters.
     */
    suspend fun listFolder(
        folderId: String?,
        userId: String,
        query: StorageItemQuery,
    ): Either<StorageException, PagedResult<StorageItem>> {
        if (folderId != null) {
            when (val result = getItem(folderId, userId)) {
                is Either.Left -> return result
                is Either.Right -> {
                    if (result.value.type != ItemType.FOLDER) {
                        return ValidationException("Not a folder").left()
                    }
                }
            }
        }

        val adjustedQuery = query.copy(
            parentId = folderId,
            ownerId = userId,
            filterByParent = true,
        )

        return storageItemRepository.query(adjustedQuery)
    }

    /**
     * Downloads a file.
     */
    suspend fun downloadFile(itemId: String, userId: String): Either<StorageException, Pair<StorageItem, InputStream>> {
        val item = when (val result = getItem(itemId, userId)) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }

        if (item.type != ItemType.FILE) {
            return ValidationException("Cannot download a folder").left()
        }

        val storageKey = item.storageKey ?: return ValidationException("File has no storage key").left()

        return when (val result = storageBackend.retrieve(storageKey)) {
            is Either.Left -> result
            is Either.Right -> {
                eventBus.publish(FileEvent.Downloaded(userId = userId, item = item))
                Pair(item, result.value).right()
            }
        }
    }

    /**
     * Renames an item.
     */
    suspend fun renameItem(itemId: String, newName: String, userId: String): Either<StorageException, StorageItem> =
        renameItemInternal(
            storageItemRepository,
            { i, u -> getItem(i, u) },
            itemId,
            newName,
            userId,
        )

    /**
     * Moves an item to a new location.
     */
    suspend fun moveItem(input: MoveItemInput): Either<StorageException, StorageItem> =
        moveItemInternal(storageItemRepository, { i, u -> getItem(i, u) }, input)

    /**
     * Copies an item.
     */
    suspend fun copyItem(input: CopyItemInput): Either<StorageException, StorageItem> =
        copyItemInternal(storageItemRepository, storageBackend, { i, u -> getItem(i, u) }, input)

    /**
     * Toggles star status.
     */
    suspend fun toggleStar(itemId: String, userId: String): Either<StorageException, StorageItem> =
        toggleStarInternal(storageItemRepository, { i, u -> getItem(i, u) }, itemId, userId)

    /**
     * Sets star status to a specific value.
     */
    suspend fun setStar(itemId: String, userId: String, starred: Boolean): Either<StorageException, StorageItem> =
        setStarInternal(storageItemRepository, { i, u -> getItem(i, u) }, itemId, userId, starred)

    /**
     * Moves item to trash.
     */
    suspend fun trashItem(itemId: String, userId: String): Either<StorageException, StorageItem> =
        trashItemInternal(storageItemRepository, eventBus, { i, u -> getItem(i, u) }, itemId, userId)

    /**
     * Restores item from trash.
     */
    suspend fun restoreItem(itemId: String, userId: String): Either<StorageException, StorageItem> =
        restoreItemInternal(storageItemRepository, eventBus, { i, u -> getItem(i, u) }, itemId, userId)

    /**
     * Permanently deletes an item.
     */
    suspend fun deleteItem(itemId: String, userId: String): Either<StorageException, Unit> =
        deleteItemInternal(
            storageItemRepository,
            storageBackend,
            eventBus,
            { i, u -> getItem(i, u) },
            itemId,
            userId,
        )

    /**
     * Gets trashed items.
     */
    suspend fun getTrashItems(userId: String): Either<StorageException, List<StorageItem>> {
        val query = StorageItemQuery(
            ownerId = userId,
            isTrashed = true,
        )
        return storageItemRepository.query(query).map { it.items }
    }

    /**
     * Gets starred items.
     */
    suspend fun getStarredItems(userId: String): Either<StorageException, List<StorageItem>> {
        val query = StorageItemQuery(
            ownerId = userId,
            isStarred = true,
            isTrashed = false,
        )
        return storageItemRepository.query(query).map { it.items }
    }

    /**
     * Gets recent items.
     */
    suspend fun getRecentItems(userId: String, limit: Int = 20): Either<StorageException, List<StorageItem>> {
        val query = StorageItemQuery(
            ownerId = userId,
            isTrashed = false,
            sortField = SortField.UPDATED_AT,
            sortOrder = SortOrder.DESC,
            limit = limit,
        )
        return storageItemRepository.query(query).map { it.items }
    }

    /**
     * Gets breadcrumbs (ancestors) for an item.
     */
    suspend fun getBreadcrumbs(itemId: String, userId: String): Either<StorageException, List<StorageItem>> {
        when (val result = getItem(itemId, userId)) {
            is Either.Left -> return result
            is Either.Right -> { /* continue */ }
        }

        return storageItemRepository.getAncestors(itemId)
    }

    /**
     * Searches for items.
     */
    suspend fun search(
        query: String,
        userId: String,
        limit: Int = 50,
        offset: Int = 0,
    ): Either<StorageException, PagedResult<StorageItem>> {
        val searchQuery = StorageItemQuery(
            ownerId = userId,
            searchQuery = query,
            isTrashed = false,
            limit = limit,
            offset = offset,
        )
        return storageItemRepository.query(searchQuery)
    }

    /**
     * Gets quota for a user.
     */
    suspend fun getQuota(userId: String): Either<StorageException, StorageQuota> {
        return storageItemRepository.getQuota(userId)
    }
}
