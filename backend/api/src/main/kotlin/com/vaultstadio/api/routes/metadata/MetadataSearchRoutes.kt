package com.vaultstadio.api.routes.metadata

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.PaginatedResponse
import com.vaultstadio.api.routes.storage.SelectionSurface
import com.vaultstadio.api.routes.storage.storageSelection
import com.vaultstadio.core.domain.model.StorageItemMetadata
import com.vaultstadio.core.domain.repository.MetadataRepository
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.get as koinGet

internal fun Route.metadataSearchRoutes() {
    // Search by metadata
    get("/by-metadata") {
        val storageService: StorageService = call.application.koinGet()
        val metadataRepository: MetadataRepository = call.application.koinGet()
        val user = call.user!!
        val key = call.request.queryParameters["key"]
        val value = call.request.queryParameters["value"]
        val pluginId = call.request.queryParameters["pluginId"]
        val selection = call.storageSelection(SelectionSurface.METADATA)

        if (key.isNullOrBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(
                    success = false,
                    error = ApiError("INVALID_KEY", "Metadata key is required"),
                ),
            )
            return@get
        }

        // Search by metadata key/value
        val valuePattern = value ?: "%"

        val searchResult = metadataRepository.searchByKeyValue(
            key = key,
            valuePattern = valuePattern,
            pluginId = pluginId,
            limit = selection.page.limit,
        )

        searchResult.fold(
            { error -> throw error },
            { metadataList ->
                val results = ownedMetadataResults(storageService, metadataList, user.id)

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = PaginatedResponse(
                            items = results,
                            total = results.size.toLong(),
                            page = 1,
                            pageSize = selection.page.limit,
                            totalPages = if (results.isEmpty()) 0L else 1L,
                            hasMore = false,
                        ),
                    ),
                )
            },
        )
    }
}

private suspend fun ownedMetadataResults(
    storage: StorageService,
    records: List<StorageItemMetadata>,
    actorId: String,
): List<MetadataSearchResult> = records.mapNotNull { metadata ->
    storage.getItem(metadata.itemId, actorId).fold(
        { failure ->
            when (failure) {
                is AuthorizationException, is ItemNotFoundException -> null
                else -> throw failure
            }
        },
        { item ->
            MetadataSearchResult(item.id, item.name, item.path, metadata.pluginId, metadata.key, metadata.value)
        },
    )
}
