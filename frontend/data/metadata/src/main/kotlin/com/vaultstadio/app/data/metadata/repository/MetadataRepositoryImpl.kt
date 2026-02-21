/**
 * Metadata Repository implementation
 */

package com.vaultstadio.app.data.metadata.repository

import com.vaultstadio.app.data.metadata.service.MetadataService
import com.vaultstadio.app.data.network.mapper.toResult
import com.vaultstadio.app.domain.metadata.MetadataRepository
import com.vaultstadio.app.domain.metadata.model.DocumentMetadata
import com.vaultstadio.app.domain.metadata.model.FileMetadata
import com.vaultstadio.app.domain.metadata.model.ImageMetadata
import com.vaultstadio.app.domain.metadata.model.MetadataSearchResult
import com.vaultstadio.app.domain.metadata.model.VideoMetadata
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.PaginatedResponse
import com.vaultstadio.app.domain.storage.model.StorageItem
import kotlinx.datetime.Instant

class MetadataRepositoryImpl(
    private val metadataService: MetadataService,
) : MetadataRepository {

    override suspend fun getFileMetadata(itemId: String) =
        metadataService.getFileMetadata(itemId).toResult()

    override suspend fun getImageMetadata(itemId: String) =
        metadataService.getImageMetadata(itemId).toResult()

    override suspend fun getVideoMetadata(itemId: String) =
        metadataService.getVideoMetadata(itemId).toResult()

    override suspend fun getDocumentMetadata(itemId: String) =
        metadataService.getDocumentMetadata(itemId).toResult()

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
    ): Result<PaginatedResponse<StorageItem>> =
        metadataService.advancedSearch(
            query,
            searchContent,
            fileTypes,
            minSize,
            maxSize,
            fromDate,
            toDate,
            limit,
            offset,
        ).toResult()

    override suspend fun searchByMetadata(
        key: String,
        value: String?,
        pluginId: String?,
        limit: Int,
    ) = metadataService.searchByMetadata(key, value, pluginId, limit).toResult()

    override suspend fun getSearchSuggestions(prefix: String, limit: Int) =
        metadataService.getSearchSuggestions(prefix, limit).toResult()
}
