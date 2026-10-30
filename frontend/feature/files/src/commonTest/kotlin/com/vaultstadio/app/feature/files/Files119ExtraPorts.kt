package com.vaultstadio.app.feature.files

import com.vaultstadio.app.domain.activity.model.Activity
import com.vaultstadio.app.domain.activity.usecase.GetItemActivityUseCase
import com.vaultstadio.app.domain.config.usecase.GetShareUrlUseCase
import com.vaultstadio.app.domain.config.usecase.GetStorageUrlsUseCase
import com.vaultstadio.app.domain.metadata.usecase.GetSearchSuggestionsUseCase
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.share.model.ShareLink
import com.vaultstadio.app.domain.share.usecase.CreateShareUseCase
import com.vaultstadio.app.domain.storage.model.PaginatedResponse
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.domain.storage.usecase.SearchUseCase

internal class Files119ExtraPorts {
    val calls = mutableListOf<Files119Mutation>()
    var searchResult: Result<PaginatedResponse<StorageItem>> = Result.Success(
        files119Page(listOf(files119Item("found119"))),
    )
    var suggestionResult: Result<List<String>> = Result.Success(listOf("found119"))
    var activityResult: suspend (String) -> Result<List<Activity>> = { Result.Success(emptyList()) }
    val shared =
        ShareLink(
            "share119", "file119", "owned-ephemeral-share",
            "https://audit.invalid/s/owned-ephemeral-share", null, false, null, 0, true, files119Time,
        )
    var shareResult: Result<ShareLink> = Result.Success(shared)
    var beforeShare: suspend () -> Unit = {}
    val search = object : SearchUseCase {
        override suspend fun invoke(query: String, limit: Int, offset: Int): Result<PaginatedResponse<StorageItem>> {
            calls += Files119Mutation("search", listOf(query, limit, offset))
            return searchResult
        }
    }
    val suggestions = object : GetSearchSuggestionsUseCase {
        override suspend fun invoke(prefix: String, limit: Int): Result<List<String>> {
            calls += Files119Mutation("suggestions", listOf(prefix, limit))
            return suggestionResult
        }
    }
    val activity = object : GetItemActivityUseCase {
        override suspend fun invoke(itemId: String, limit: Int): Result<List<Activity>> {
            calls += Files119Mutation("activity", listOf(itemId, limit))
            return activityResult(itemId)
        }
    }
    val share = object : CreateShareUseCase {
        override suspend fun invoke(
            itemId: String,
            expiresInDays: Int?,
            password: String?,
            maxDownloads: Int?,
        ): Result<ShareLink> {
            calls += Files119Mutation("share", listOf(itemId, expiresInDays, password, maxDownloads))
            beforeShare()
            return shareResult
        }
    }
    val urls = object : GetStorageUrlsUseCase {
        override fun downloadUrl(itemId: String) = "download:$itemId"
        override fun thumbnailUrl(itemId: String, size: String) = "thumbnail:$itemId:$size"
        override fun previewUrl(itemId: String) = "preview:$itemId"
        override fun batchDownloadZipUrl() = "download-zip"
    }
    val shareUrl = object : GetShareUrlUseCase {
        override fun invoke(token: String) = "share:$token"
    }
}
