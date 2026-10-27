package com.vaultstadio.app.feature.files

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.Breadcrumb
import com.vaultstadio.app.domain.storage.model.PaginatedResponse
import com.vaultstadio.app.domain.storage.model.SortField
import com.vaultstadio.app.domain.storage.model.SortOrder
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.domain.storage.usecase.GetBreadcrumbsUseCase
import com.vaultstadio.app.domain.storage.usecase.GetFolderItemsUseCase
import com.vaultstadio.app.domain.storage.usecase.GetRecentUseCase
import com.vaultstadio.app.domain.storage.usecase.GetStarredUseCase
import com.vaultstadio.app.domain.storage.usecase.GetTrashUseCase

internal data class Files119Query(val folder: String?, val sort: SortField, val order: SortOrder, val limit: Int, val offset: Int)
internal class Files119LoadingPorts {
    val queries = mutableListOf<Files119Query>()
    val modes = mutableListOf<String>()
    var folderResult: suspend (Files119Query) -> Result<PaginatedResponse<StorageItem>> = { Result.Success(files119Page()) }
    var recentResult: Result<List<StorageItem>> = Result.Success(listOf(files119Item("recent119")))
    var starredResult: Result<List<StorageItem>> = Result.Success(listOf(files119Item("star119")))
    var trashResult: Result<List<StorageItem>> = Result.Success(listOf(files119Item("trash119")))
    var breadcrumbResult: Result<List<Breadcrumb>> = Result.Success(emptyList())
    val folder = object : GetFolderItemsUseCase {
        override suspend fun invoke(folderId: String?, sortBy: SortField, sortOrder: SortOrder, limit: Int, offset: Int): Result<PaginatedResponse<StorageItem>> {
            val query = Files119Query(folderId, sortBy, sortOrder, limit, offset)
            queries += query
            return folderResult(query)
        }
    }
    val recent = object : GetRecentUseCase {
        override suspend fun invoke(limit: Int): Result<List<StorageItem>> { modes += "recent"; return recentResult }
    }
    val starred = object : GetStarredUseCase {
        override suspend fun invoke(): Result<List<StorageItem>> { modes += "starred"; return starredResult }
    }
    val trash = object : GetTrashUseCase {
        override suspend fun invoke(): Result<List<StorageItem>> { modes += "trash"; return trashResult }
    }
    val breadcrumbs = object : GetBreadcrumbsUseCase {
        override suspend fun invoke(itemId: String): Result<List<Breadcrumb>> { modes += "breadcrumbs:$itemId"; return breadcrumbResult }
    }
    fun loader(pageSize: Int = 2) = FilesLoader(folder, recent, starred, trash, breadcrumbs, pageSize)
}
