/**
 * Repository interface for metadata operations.
 */

package com.vaultstadio.app.domain.metadata

import com.vaultstadio.app.domain.metadata.model.DocumentMetadata
import com.vaultstadio.app.domain.metadata.model.FileMetadata
import com.vaultstadio.app.domain.metadata.model.ImageMetadata
import com.vaultstadio.app.domain.metadata.model.MetadataSearchResult
import com.vaultstadio.app.domain.metadata.model.VideoMetadata
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.PaginatedResponse
import com.vaultstadio.app.domain.storage.model.StorageItem
import kotlinx.datetime.Instant

interface MetadataRepository {
    suspend fun getFileMetadata(itemId: String): Result<FileMetadata>
    suspend fun getImageMetadata(itemId: String): Result<ImageMetadata>
    suspend fun getVideoMetadata(itemId: String): Result<VideoMetadata>
    suspend fun getDocumentMetadata(itemId: String): Result<DocumentMetadata>
    suspend fun advancedSearch(
        query: String,
        searchContent: Boolean = false,
        fileTypes: List<String>? = null,
        minSize: Long? = null,
        maxSize: Long? = null,
        fromDate: Instant? = null,
        toDate: Instant? = null,
        limit: Int = 50,
        offset: Int = 0,
    ): Result<PaginatedResponse<StorageItem>>
    suspend fun searchByMetadata(
        key: String,
        value: String? = null,
        pluginId: String? = null,
        limit: Int = 50,
    ): Result<PaginatedResponse<MetadataSearchResult>>
    suspend fun getSearchSuggestions(prefix: String, limit: Int = 10): Result<List<String>>
}
