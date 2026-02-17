/**
 * Metadata Repository
 */

package com.vaultstadio.app.data.repository

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.service.MetadataService
import com.vaultstadio.app.domain.model.DocumentMetadata
import com.vaultstadio.app.domain.model.FileMetadata
import com.vaultstadio.app.domain.model.ImageMetadata
import com.vaultstadio.app.domain.model.MetadataSearchResult
import com.vaultstadio.app.domain.model.PaginatedResponse
import com.vaultstadio.app.domain.model.StorageItem
import com.vaultstadio.app.domain.model.VideoMetadata
import kotlinx.datetime.Instant
import org.koin.core.annotation.Single

/**
 * Repository interface for metadata operations.
 */
interface MetadataRepository {
    suspend fun getFileMetadata(itemId: String): ApiResult<FileMetadata>
    suspend fun getImageMetadata(itemId: String): ApiResult<ImageMetadata>
    suspend fun getVideoMetadata(itemId: String): ApiResult<VideoMetadata>
    suspend fun getDocumentMetadata(itemId: String): ApiResult<DocumentMetadata>
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
    ): ApiResult<PaginatedResponse<StorageItem>>
    suspend fun searchByMetadata(
        key: String,
        value: String? = null,
        pluginId: String? = null,
        limit: Int = 50,
    ): ApiResult<PaginatedResponse<MetadataSearchResult>>
    suspend fun getSearchSuggestions(prefix: String, limit: Int = 10): ApiResult<List<String>>
}

@Single(binds = [MetadataRepository::class])
class MetadataRepositoryImpl(
    private val metadataService: MetadataService,
) : MetadataRepository {

    override suspend fun getFileMetadata(itemId: String) = metadataService.getFileMetadata(itemId)
    override suspend fun getImageMetadata(itemId: String) = metadataService.getImageMetadata(itemId)
    override suspend fun getVideoMetadata(itemId: String) = metadataService.getVideoMetadata(itemId)
    override suspend fun getDocumentMetadata(itemId: String) = metadataService.getDocumentMetadata(itemId)

    override suspend fun advancedSearch(
        query: String,
        searchContent: Boolean,
        fileTypes: List<String>?,
        minSize: Long?,
        maxSize: Long?,
        fromDate: Instant?,
        toDate: Instant?,
        limit: Int,
        offset: Int,
    ): ApiResult<PaginatedResponse<StorageItem>> = metadataService.advancedSearch(
        query,
        searchContent,
        fileTypes,
        minSize,
        maxSize,
        fromDate,
        toDate,
        limit,
        offset,
    )

    override suspend fun searchByMetadata(key: String, value: String?, pluginId: String?, limit: Int) =
        metadataService.searchByMetadata(key, value, pluginId, limit)

    override suspend fun getSearchSuggestions(
        prefix: String,
        limit: Int,
    ) = metadataService.getSearchSuggestions(prefix, limit)
}
