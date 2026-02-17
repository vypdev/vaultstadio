/**
 * VaultStadio File Version Repository
 *
 * Interface for file version persistence operations.
 */

package com.vaultstadio.core.domain.repository

import arrow.core.Either
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.model.FileVersionHistory
import com.vaultstadio.core.domain.model.VersionRetentionPolicy
import com.vaultstadio.core.exception.StorageException
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for file versions.
 */
interface FileVersionRepository {

    /**
     * Create a new version for a file.
     *
     * @param version The version to create
     * @return The created version or an error
     */
    suspend fun create(version: FileVersion): Either<StorageException, FileVersion>

    /**
     * Find a specific version by ID.
     *
     * @param id Version ID
     * @return The version or null if not found
     */
    suspend fun findById(id: String): Either<StorageException, FileVersion?>

    /**
     * Find a specific version by item ID and version number.
     *
     * @param itemId Storage item ID
     * @param versionNumber Version number
     * @return The version or null if not found
     */
    suspend fun findByItemAndVersion(
        itemId: String,
        versionNumber: Int,
    ): Either<StorageException, FileVersion?>

    /**
     * Get the latest version for an item.
     *
     * @param itemId Storage item ID
     * @return The latest version or null if no versions exist
     */
    suspend fun findLatest(itemId: String): Either<StorageException, FileVersion?>

    /**
     * List all versions for an item.
     *
     * @param itemId Storage item ID
     * @param limit Maximum number of versions to return
     * @param offset Offset for pagination
     * @return List of versions, newest first
     */
    suspend fun listVersions(
        itemId: String,
        limit: Int = 100,
        offset: Int = 0,
    ): Either<StorageException, List<FileVersion>>

    /**
     * Get complete version history for an item.
     *
     * @param itemId Storage item ID
     * @return Version history with metadata
     */
    suspend fun getHistory(itemId: String): Either<StorageException, FileVersionHistory>

    /**
     * Get the next version number for an item.
     *
     * @param itemId Storage item ID
     * @return The next version number
     */
    suspend fun getNextVersionNumber(itemId: String): Either<StorageException, Int>

    /**
     * Update a version (e.g., mark as not latest).
     *
     * @param version The updated version
     * @return The updated version
     */
    suspend fun update(version: FileVersion): Either<StorageException, FileVersion>

    /**
     * Delete a specific version.
     *
     * @param id Version ID
     * @return Unit on success
     */
    suspend fun delete(id: String): Either<StorageException, Unit>

    /**
     * Delete all versions for an item.
     *
     * @param itemId Storage item ID
     * @return Number of versions deleted
     */
    suspend fun deleteAllForItem(itemId: String): Either<StorageException, Int>

    /**
     * Apply retention policy to clean up old versions.
     *
     * @param itemId Storage item ID
     * @param policy Retention policy to apply
     * @return List of version IDs that were deleted
     */
    suspend fun applyRetentionPolicy(
        itemId: String,
        policy: VersionRetentionPolicy,
    ): Either<StorageException, List<String>>

    /**
     * Get total version count for an item.
     *
     * @param itemId Storage item ID
     * @return Number of versions
     */
    suspend fun countVersions(itemId: String): Either<StorageException, Int>

    /**
     * Get total size of all versions for an item.
     *
     * @param itemId Storage item ID
     * @return Total size in bytes
     */
    suspend fun getTotalVersionSize(itemId: String): Either<StorageException, Long>

    /**
     * Stream all versions (for batch processing).
     *
     * @param itemId Optional item ID to filter by
     * @return Flow of versions
     */
    fun streamVersions(itemId: String? = null): Flow<FileVersion>

    /**
     * Find versions by storage key (for cleanup).
     *
     * @param storageKey Storage key
     * @return List of versions using this storage key
     */
    suspend fun findByStorageKey(storageKey: String): Either<StorageException, List<FileVersion>>
}
