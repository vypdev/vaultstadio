/**
 * Metadata API
 */

package com.vaultstadio.app.data.metadata.api

import com.vaultstadio.app.data.metadata.dto.AdvancedSearchRequestDTO
import com.vaultstadio.app.data.metadata.dto.DocumentMetadataDTO
import com.vaultstadio.app.data.metadata.dto.FileMetadataDTO
import com.vaultstadio.app.data.metadata.dto.ImageMetadataDTO
import com.vaultstadio.app.data.metadata.dto.MetadataSearchResultDTO
import com.vaultstadio.app.data.metadata.dto.VideoMetadataDTO
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.BaseApi
import com.vaultstadio.app.data.network.dto.common.PaginatedResponseDTO
import com.vaultstadio.app.data.storage.dto.StorageItemDTO
import io.ktor.client.HttpClient

class MetadataApi(client: HttpClient) : BaseApi(client) {

    suspend fun getFileMetadata(itemId: String): ApiResult<FileMetadataDTO> =
        get("/api/v1/storage/item/$itemId/metadata")

    suspend fun getImageMetadata(itemId: String): ApiResult<ImageMetadataDTO> =
        get("/api/v1/storage/item/$itemId/metadata/image")

    suspend fun getVideoMetadata(itemId: String): ApiResult<VideoMetadataDTO> =
        get("/api/v1/storage/item/$itemId/metadata/video")

    suspend fun getDocumentMetadata(itemId: String): ApiResult<DocumentMetadataDTO> =
        get("/api/v1/storage/item/$itemId/metadata/document")

    suspend fun advancedSearch(request: AdvancedSearchRequestDTO): ApiResult<PaginatedResponseDTO<StorageItemDTO>> =
        post("/api/v1/search/advanced", request)

    suspend fun searchByMetadata(
        key: String,
        value: String?,
        pluginId: String?,
        limit: Int,
    ): ApiResult<PaginatedResponseDTO<MetadataSearchResultDTO>> {
        val params = mutableMapOf("key" to key, "limit" to limit.toString())
        value?.let { params["value"] = it }
        pluginId?.let { params["pluginId"] = it }
        return get("/api/v1/search/by-metadata", params)
    }

    suspend fun getSearchSuggestions(prefix: String, limit: Int = 10): ApiResult<List<String>> =
        get("/api/v1/search/suggestions", mapOf("prefix" to prefix, "limit" to limit.toString()))
}
