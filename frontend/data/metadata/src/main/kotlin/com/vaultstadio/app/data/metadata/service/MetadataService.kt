/**
 * Metadata Service
 */

package com.vaultstadio.app.data.metadata.service

import com.vaultstadio.app.data.metadata.api.MetadataApi
import com.vaultstadio.app.data.metadata.dto.AdvancedSearchRequestDTO
import com.vaultstadio.app.data.metadata.mapper.toDomain
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.metadata.model.DocumentMetadata
import com.vaultstadio.app.domain.metadata.model.FileMetadata
import com.vaultstadio.app.domain.metadata.model.ImageMetadata
import com.vaultstadio.app.domain.metadata.model.MetadataSearchResult
import com.vaultstadio.app.domain.metadata.model.VideoMetadata
import com.vaultstadio.app.domain.storage.model.PaginatedResponse
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.data.storage.mapper.toDomain as storageItemToDomain
import kotlinx.datetime.Instant

class MetadataService(private val metadataApi: MetadataApi) {

    suspend fun getFileMetadata(itemId: String): ApiResult<FileMetadata> =
        metadataApi.getFileMetadata(itemId).map { it.toDomain() }

    suspend fun getImageMetadata(itemId: String): ApiResult<ImageMetadata> =
        metadataApi.getImageMetadata(itemId).map { it.toDomain() }

    suspend fun getVideoMetadata(itemId: String): ApiResult<VideoMetadata> =
        metadataApi.getVideoMetadata(itemId).map { it.toDomain() }

    suspend fun getDocumentMetadata(itemId: String): ApiResult<DocumentMetadata> =
        metadataApi.getDocumentMetadata(itemId).map { it.toDomain() }

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
    ): ApiResult<PaginatedResponse<StorageItem>> =
        metadataApi.advancedSearch(
            AdvancedSearchRequestDTO(
                query, searchContent, fileTypes, minSize, maxSize, fromDate, toDate, limit, offset,
            ),
        ).map { dto ->
            PaginatedResponse(
                dto.items.map { it.storageItemToDomain() },
                dto.total,
                dto.page,
                dto.pageSize,
                dto.totalPages,
                dto.hasMore,
            )
        }

    suspend fun searchByMetadata(
        key: String,
        value: String? = null,
        pluginId: String? = null,
        limit: Int = 50,
    ): ApiResult<PaginatedResponse<MetadataSearchResult>> =
        metadataApi.searchByMetadata(key, value, pluginId, limit).map { dto ->
            PaginatedResponse(
                dto.items.map { it.toDomain() },
                dto.total,
                dto.page,
                dto.pageSize,
                dto.totalPages,
                dto.hasMore,
            )
        }

    suspend fun getSearchSuggestions(prefix: String, limit: Int = 10): ApiResult<List<String>> =
        metadataApi.getSearchSuggestions(prefix, limit)
}
