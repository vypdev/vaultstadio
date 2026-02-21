/**
 * VaultStadio Metadata Repository
 *
 * Interface for plugin-attached metadata persistence.
 */

package com.vaultstadio.core.domain.repository

import arrow.core.Either
import com.vaultstadio.core.domain.model.StorageItemMetadata
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.pagination.PagedResult

/**
 * Repository interface for storage item metadata.
 *
 * This repository handles the extended metadata that plugins attach
 * to storage items. Each piece of metadata is identified by a combination
 * of itemId, pluginId, and key.
 */
interface MetadataRepository {

    /**
     * Creates or updates metadata for an item.
     *
     * @param metadata The metadata to save
     * @return Either an error or the saved metadata
     */
    suspend fun save(metadata: StorageItemMetadata): Either<StorageException, StorageItemMetadata>

    /**
     * Saves multiple metadata entries at once.
     *
     * @param metadataList List of metadata to save
     * @return Either an error or the saved metadata list
     */
    suspend fun saveAll(metadataList: List<StorageItemMetadata>): Either<StorageException, List<StorageItemMetadata>>

    /**
     * Finds metadata by ID.
     *
     * @param id Metadata ID
     * @return Either an error or the metadata (null if not found)
     */
    suspend fun findById(id: String): Either<StorageException, StorageItemMetadata?>

    /**
     * Finds all metadata for a storage item.
     *
     * @param itemId Storage item ID
     * @return Either an error or list of metadata
     */
    suspend fun findByItemId(itemId: String): Either<StorageException, List<StorageItemMetadata>>

    /**
     * Finds metadata for a storage item from a specific plugin.
     *
     * @param itemId Storage item ID
     * @param pluginId Plugin ID
     * @return Either an error or list of metadata
     */
    suspend fun findByItemIdAndPluginId(
        itemId: String,
        pluginId: String,
    ): Either<StorageException, List<StorageItemMetadata>>

    /**
     * Finds a specific metadata entry.
     *
     * @param itemId Storage item ID
     * @param pluginId Plugin ID
     * @param key Metadata key
     * @return Either an error or the metadata (null if not found)
     */
    suspend fun findByItemIdAndPluginIdAndKey(
        itemId: String,
        pluginId: String,
        key: String,
    ): Either<StorageException, StorageItemMetadata?>

    /**
     * Finds all metadata created by a plugin.
     *
     * @param pluginId Plugin ID
     * @param limit Maximum results
     * @param offset Pagination offset
     * @return Either an error or paginated metadata
     */
    suspend fun findByPluginId(
        pluginId: String,
        limit: Int = 100,
        offset: Int = 0,
    ): Either<StorageException, PagedResult<StorageItemMetadata>>

    /**
     * Deletes metadata by ID.
     *
     * @param id Metadata ID
     * @return Either an error or Unit on success
     */
    suspend fun delete(id: String): Either<StorageException, Unit>

    /**
     * Deletes all metadata for a storage item.
     *
     * @param itemId Storage item ID
     * @return Either an error or number of deleted entries
     */
    suspend fun deleteByItemId(itemId: String): Either<StorageException, Int>

    /**
     * Deletes all metadata from a plugin for a storage item.
     *
     * @param itemId Storage item ID
     * @param pluginId Plugin ID
     * @return Either an error or number of deleted entries
     */
    suspend fun deleteByItemIdAndPluginId(
        itemId: String,
        pluginId: String,
    ): Either<StorageException, Int>

    /**
     * Deletes all metadata created by a plugin.
     *
     * @param pluginId Plugin ID
     * @return Either an error or number of deleted entries
     */
    suspend fun deleteByPluginId(pluginId: String): Either<StorageException, Int>

    /**
     * Searches metadata by key-value pairs.
     *
     * Useful for plugins that need to query items based on their metadata.
     *
     * @param pluginId Plugin ID
     * @param key Metadata key to search
     * @param valuePattern JSON path pattern to match in value
     * @param limit Maximum results
     * @param offset Pagination offset
     * @return Either an error or list of item IDs matching the criteria
     */
    suspend fun searchByValue(
        pluginId: String,
        key: String,
        valuePattern: String,
        limit: Int = 100,
        offset: Int = 0,
    ): Either<StorageException, List<String>>

    /**
     * Searches metadata by key-value pairs across all plugins.
     *
     * Returns full metadata entries for more detailed results.
     *
     * @param key Metadata key to search
     * @param valuePattern Pattern to match in value (supports SQL LIKE patterns)
     * @param pluginId Optional plugin ID to filter by
     * @param limit Maximum results
     * @return Either an error or list of metadata matching the criteria
     */
    suspend fun searchByKeyValue(
        key: String,
        valuePattern: String,
        pluginId: String? = null,
        limit: Int = 100,
    ): Either<StorageException, List<StorageItemMetadata>>
}
