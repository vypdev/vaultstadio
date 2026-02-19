/**
 * VaultStadio File Version Service
 *
 * Business logic for file versioning operations.
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.DiffOperation
import com.vaultstadio.core.domain.model.DiffPatch
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.model.FileVersionHistory
import com.vaultstadio.core.domain.model.ItemType
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.model.VersionDiff
import com.vaultstadio.core.domain.model.VersionRetentionPolicy
import com.vaultstadio.core.domain.repository.FileVersionRepository
import com.vaultstadio.core.domain.repository.StorageItemRepository
import com.vaultstadio.core.exception.InvalidOperationException
import com.vaultstadio.core.exception.ItemNotFoundException
import com.vaultstadio.core.exception.StorageException
import kotlinx.datetime.Clock
import java.util.UUID

/**
 * Input for creating a new file version.
 *
 * @property itemId Storage item ID
 * @property size File size in bytes
 * @property checksum File checksum
 * @property storageKey Storage backend key
 * @property comment Optional version comment
 */
data class CreateVersionInput(
    val itemId: String,
    val size: Long,
    val checksum: String,
    val storageKey: String,
    val comment: String? = null,
)

/**
 * Input for restoring a file version.
 *
 * @property itemId Storage item ID
 * @property versionNumber Version number to restore
 * @property comment Optional comment for the restore
 */
data class RestoreVersionInput(
    val itemId: String,
    val versionNumber: Int,
    val comment: String? = null,
)

/**
 * Service for managing file versions.
 *
 * @property versionRepository Repository for version persistence
 * @property itemRepository Repository for storage items
 * @property storageBackend Storage backend for file operations
 */
class FileVersionService(
    private val versionRepository: FileVersionRepository,
    private val itemRepository: StorageItemRepository,
    private val storageBackend: StorageBackend,
) {

    /**
     * Create a new version when a file is updated.
     *
     * @param input Version creation input
     * @param userId User creating the version
     * @return The created version
     */
    suspend fun createVersion(
        input: CreateVersionInput,
        userId: String,
    ): Either<StorageException, FileVersion> {
        // Verify item exists and is a file
        return itemRepository.findById(input.itemId).flatMap { item ->
            when {
                item == null -> ItemNotFoundException(
                    itemId = input.itemId,
                ).left()
                item.type != ItemType.FILE -> InvalidOperationException(
                    operation = "createVersion",
                    message = "Versioning only applies to files",
                ).left()
                else -> createVersionInternal(item, input, userId)
            }
        }
    }

    private suspend fun createVersionInternal(
        item: StorageItem,
        input: CreateVersionInput,
        userId: String,
    ): Either<StorageException, FileVersion> {
        // Get next version number
        return versionRepository.getNextVersionNumber(input.itemId).flatMap { nextVersion ->
            // Mark current latest as not latest
            versionRepository.findLatest(input.itemId).flatMap { currentLatest ->
                val updateResult = if (currentLatest != null) {
                    versionRepository.update(currentLatest.copy(isLatest = false))
                } else {
                    Unit.right()
                }

                updateResult.flatMap {
                    // Create new version
                    val version = FileVersion(
                        id = UUID.randomUUID().toString(),
                        itemId = input.itemId,
                        versionNumber = nextVersion,
                        size = input.size,
                        checksum = input.checksum,
                        storageKey = input.storageKey,
                        createdBy = userId,
                        createdAt = Clock.System.now(),
                        comment = input.comment,
                        isLatest = true,
                    )

                    versionRepository.create(version)
                }
            }
        }
    }

    /**
     * Restore a previous version, creating a new version from it.
     *
     * @param input Restore input
     * @param userId User performing the restore
     * @return The new version created from the restore
     */
    suspend fun restoreVersion(
        input: RestoreVersionInput,
        userId: String,
    ): Either<StorageException, FileVersion> {
        return versionRepository.findByItemAndVersion(
            input.itemId,
            input.versionNumber,
        ).flatMap { versionToRestore ->
            when {
                versionToRestore == null -> ItemNotFoundException(
                    itemId = input.itemId,
                    message = "Version ${input.versionNumber} not found for item ${input.itemId}",
                ).left()
                versionToRestore.isLatest -> InvalidOperationException(
                    operation = "restoreVersion",
                    message = "Cannot restore the current version",
                ).left()
                else -> {
                    // Create a new version from the restored one
                    val comment = input.comment
                        ?: "Restored from version ${input.versionNumber}"

                    createVersion(
                        CreateVersionInput(
                            itemId = input.itemId,
                            size = versionToRestore.size,
                            checksum = versionToRestore.checksum,
                            storageKey = versionToRestore.storageKey,
                            comment = comment,
                        ),
                        userId,
                    ).map { newVersion ->
                        newVersion.copy(restoredFrom = input.versionNumber)
                    }
                }
            }
        }
    }

    /**
     * Get version history for a file.
     *
     * @param itemId Storage item ID
     * @return Version history
     */
    suspend fun getHistory(itemId: String): Either<StorageException, FileVersionHistory> {
        return versionRepository.getHistory(itemId)
    }

    /**
     * Get a specific version.
     *
     * @param itemId Storage item ID
     * @param versionNumber Version number
     * @return The version
     */
    suspend fun getVersion(
        itemId: String,
        versionNumber: Int,
    ): Either<StorageException, FileVersion> {
        return versionRepository.findByItemAndVersion(itemId, versionNumber).flatMap { version ->
            version?.right() ?: ItemNotFoundException(
                message = "Version $versionNumber not found",
            ).left()
        }
    }

    /**
     * Get the latest version.
     *
     * @param itemId Storage item ID
     * @return The latest version
     */
    suspend fun getLatestVersion(itemId: String): Either<StorageException, FileVersion> {
        return versionRepository.findLatest(itemId).flatMap { version ->
            version?.right() ?: ItemNotFoundException(
                itemId = itemId,
                message = "No versions found for item $itemId",
            ).left()
        }
    }

    /**
     * List all versions for a file.
     *
     * @param itemId Storage item ID
     * @param limit Maximum number of versions
     * @param offset Pagination offset
     * @return List of versions
     */
    suspend fun listVersions(
        itemId: String,
        limit: Int = 100,
        offset: Int = 0,
    ): Either<StorageException, List<FileVersion>> {
        return versionRepository.listVersions(itemId, limit, offset)
    }

    /**
     * Delete a specific version.
     *
     * @param versionId Version ID
     * @param userId User performing the delete
     * @return Unit on success
     */
    suspend fun deleteVersion(
        versionId: String,
        userId: String,
    ): Either<StorageException, Unit> {
        return versionRepository.findById(versionId).flatMap { version ->
            when {
                version == null -> ItemNotFoundException(
                    itemId = versionId,
                    message = "Version not found: $versionId",
                ).left()
                version.isLatest -> InvalidOperationException(
                    operation = "deleteVersion",
                    message = "Cannot delete the current version",
                ).left()
                else -> {
                    // Delete the version file from storage
                    storageBackend.delete(version.storageKey)
                    versionRepository.delete(versionId)
                }
            }
        }
    }

    /**
     * Apply retention policy to clean up old versions.
     *
     * @param itemId Storage item ID
     * @param policy Retention policy
     * @return List of deleted version IDs
     */
    suspend fun applyRetentionPolicy(
        itemId: String,
        policy: VersionRetentionPolicy = VersionRetentionPolicy.DEFAULT,
    ): Either<StorageException, List<String>> {
        return versionRepository.applyRetentionPolicy(itemId, policy).flatMap { deletedIds ->
            // Delete files from storage
            deletedIds.forEach { versionId ->
                versionRepository.findById(versionId).map { version ->
                    version?.let { storageBackend.delete(it.storageKey) }
                }
            }
            deletedIds.right()
        }
    }

    /**
     * Compare two versions and generate a diff.
     *
     * @param itemId Storage item ID
     * @param fromVersion Source version number
     * @param toVersion Target version number
     * @return The diff between versions
     */
    suspend fun compareVersions(
        itemId: String,
        fromVersion: Int,
        toVersion: Int,
    ): Either<StorageException, VersionDiff> {
        return versionRepository.findByItemAndVersion(itemId, fromVersion).flatMap { from ->
            versionRepository.findByItemAndVersion(itemId, toVersion).flatMap { to ->
                when {
                    from == null -> ItemNotFoundException(
                        message = "Version $fromVersion not found",
                    ).left()
                    to == null -> ItemNotFoundException(
                        message = "Version $toVersion not found",
                    ).left()
                    else -> generateDiff(from, to)
                }
            }
        }
    }

    private suspend fun generateDiff(
        from: FileVersion,
        to: FileVersion,
    ): Either<StorageException, VersionDiff> {
        val sizeChange = to.size - from.size

        // Check if files are text-based (can be diffed)
        val isBinary = !isTextFile(from) || !isTextFile(to)

        return if (isBinary) {
            // Binary files - just show size difference
            VersionDiff(
                fromVersion = from.versionNumber,
                toVersion = to.versionNumber,
                sizeChange = sizeChange,
                isBinary = true,
            ).right()
        } else {
            // Text files - generate line-by-line diff
            generateTextDiff(from, to, sizeChange)
        }
    }

    /**
     * Check if a file is a text file based on item metadata.
     */
    private suspend fun isTextFile(version: FileVersion): Boolean {
        // Get the item to check MIME type
        val itemResult = itemRepository.findById(version.itemId)
        val item = when (itemResult) {
            is Either.Left -> return false
            is Either.Right -> itemResult.value ?: return false
        }

        return isTextMimeType(item.mimeType)
    }

    /**
     * Text MIME types that support diff operations.
     */
    private val textMimeTypes = setOf(
        "text/plain",
        "text/html",
        "text/css",
        "text/javascript",
        "text/xml",
        "text/csv",
        "text/markdown",
        "text/x-python",
        "text/x-java-source",
        "text/x-kotlin",
        "text/x-c",
        "text/x-c++",
        "text/x-swift",
        "text/x-go",
        "text/x-rust",
        "text/x-ruby",
        "text/x-php",
        "text/x-sh",
        "text/x-yaml",
        "text/x-toml",
        "application/json",
        "application/xml",
        "application/javascript",
        "application/x-yaml",
        "application/toml",
    )

    /**
     * Check if a MIME type is a text type.
     */
    private fun isTextMimeType(mimeType: String?): Boolean {
        if (mimeType == null) return false

        // Check exact match
        if (textMimeTypes.contains(mimeType)) return true

        // Check prefix match
        if (mimeType.startsWith("text/")) return true

        // Check common text subtypes
        if (mimeType.endsWith("+json")) return true
        if (mimeType.endsWith("+xml")) return true

        return false
    }

    /**
     * Generate a text diff between two versions.
     * Uses a simple line-by-line diff algorithm.
     */
    private suspend fun generateTextDiff(
        from: FileVersion,
        to: FileVersion,
        sizeChange: Long,
    ): Either<StorageException, VersionDiff> {
        // Retrieve content from both versions
        val fromContentResult = storageBackend.retrieve(from.storageKey)
        val toContentResult = storageBackend.retrieve(to.storageKey)

        val fromLines = when (fromContentResult) {
            is Either.Left -> return fromContentResult
            is Either.Right -> fromContentResult.value.bufferedReader().readLines()
        }

        val toLines = when (toContentResult) {
            is Either.Left -> return toContentResult
            is Either.Right -> toContentResult.value.bufferedReader().readLines()
        }

        // Generate line-by-line diff
        val patches = generateLineDiff(fromLines, toLines)

        return VersionDiff(
            fromVersion = from.versionNumber,
            toVersion = to.versionNumber,
            sizeChange = sizeChange,
            isBinary = false,
            patches = patches,
        ).right()
    }

    /**
     * Simple line-by-line diff algorithm.
     * For production, consider using a proper diff library like diff-match-patch.
     */
    private fun generateLineDiff(fromLines: List<String>, toLines: List<String>): List<DiffPatch> {
        val patches = mutableListOf<DiffPatch>()
        var fromIndex = 0
        var toIndex = 0

        while (fromIndex < fromLines.size || toIndex < toLines.size) {
            if (fromIndex >= fromLines.size) {
                // All remaining lines in 'to' are additions
                while (toIndex < toLines.size) {
                    patches.add(
                        DiffPatch(
                            operation = DiffOperation.ADD,
                            startLine = toIndex + 1,
                            endLine = toIndex + 1,
                            newContent = toLines[toIndex],
                        ),
                    )
                    toIndex++
                }
            } else if (toIndex >= toLines.size) {
                // All remaining lines in 'from' are deletions
                while (fromIndex < fromLines.size) {
                    patches.add(
                        DiffPatch(
                            operation = DiffOperation.DELETE,
                            startLine = fromIndex + 1,
                            endLine = fromIndex + 1,
                            oldContent = fromLines[fromIndex],
                        ),
                    )
                    fromIndex++
                }
            } else if (fromLines[fromIndex] == toLines[toIndex]) {
                // Lines are equal, skip
                fromIndex++
                toIndex++
            } else {
                // Lines differ
                // Check if the line from 'from' was deleted
                val toLineInFrom = fromLines.subList(fromIndex, fromLines.size).indexOf(toLines[toIndex])
                val fromLineInTo = toLines.subList(toIndex, toLines.size).indexOf(fromLines[fromIndex])

                if (toLineInFrom == -1 && fromLineInTo == -1) {
                    // Line was replaced (modify)
                    patches.add(
                        DiffPatch(
                            operation = DiffOperation.MODIFY,
                            startLine = fromIndex + 1,
                            endLine = fromIndex + 1,
                            oldContent = fromLines[fromIndex],
                            newContent = toLines[toIndex],
                        ),
                    )
                    fromIndex++
                    toIndex++
                } else if (toLineInFrom != -1 && (fromLineInTo == -1 || toLineInFrom <= fromLineInTo)) {
                    // Lines were deleted from 'from'
                    for (i in 0 until toLineInFrom) {
                        patches.add(
                            DiffPatch(
                                operation = DiffOperation.DELETE,
                                startLine = fromIndex + i + 1,
                                endLine = fromIndex + i + 1,
                                oldContent = fromLines[fromIndex + i],
                            ),
                        )
                    }
                    fromIndex += toLineInFrom
                } else {
                    // Lines were inserted in 'to'
                    for (i in 0 until fromLineInTo) {
                        patches.add(
                            DiffPatch(
                                operation = DiffOperation.ADD,
                                startLine = toIndex + i + 1,
                                endLine = toIndex + i + 1,
                                newContent = toLines[toIndex + i],
                            ),
                        )
                    }
                    toIndex += fromLineInTo
                }
            }
        }

        return patches
    }

    /**
     * Get total version count for an item.
     *
     * @param itemId Storage item ID
     * @return Number of versions
     */
    suspend fun getVersionCount(itemId: String): Either<StorageException, Int> {
        return versionRepository.countVersions(itemId)
    }

    /**
     * Get total size of all versions for an item.
     *
     * @param itemId Storage item ID
     * @return Total size in bytes
     */
    suspend fun getTotalVersionSize(itemId: String): Either<StorageException, Long> {
        return versionRepository.getTotalVersionSize(itemId)
    }

    /**
     * Clean up versions when an item is deleted.
     *
     * @param itemId Storage item ID
     * @return Number of versions deleted
     */
    suspend fun cleanupVersionsForItem(itemId: String): Either<StorageException, Int> {
        return versionRepository.listVersions(itemId).flatMap { versions ->
            // Delete all version files from storage
            versions.forEach { version ->
                storageBackend.delete(version.storageKey)
            }
            versionRepository.deleteAllForItem(itemId)
        }
    }
}

// Note: Uses StorageBackend from StorageService.kt
