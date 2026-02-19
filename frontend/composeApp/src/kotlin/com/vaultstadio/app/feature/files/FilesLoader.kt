package com.vaultstadio.app.feature.files

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.model.Breadcrumb
import com.vaultstadio.app.domain.model.SortField
import com.vaultstadio.app.domain.model.SortOrder
import com.vaultstadio.app.domain.model.StorageItem
import com.vaultstadio.app.domain.usecase.storage.GetBreadcrumbsUseCase
import com.vaultstadio.app.domain.usecase.storage.GetFolderItemsUseCase
import com.vaultstadio.app.domain.usecase.storage.GetRecentUseCase
import com.vaultstadio.app.domain.usecase.storage.GetStarredUseCase
import com.vaultstadio.app.domain.usecase.storage.GetTrashUseCase
import com.vaultstadio.app.feature.main.MainComponent

/**
 * Result of loading folder/list items for the files screen.
 * Used by [FilesViewModel] to apply loading results to state.
 */
internal sealed class LoadItemsResult {
    data class Success(
        val items: List<StorageItem>,
        val total: Long,
        val hasMore: Boolean,
        val breadcrumbs: List<Breadcrumb>,
        val currentFolderName: String?,
    ) : LoadItemsResult()

    data class Error(val message: String) : LoadItemsResult()
}

/**
 * Result of loading the next page of folder items (pagination).
 */
internal sealed class LoadMoreResult {
    data class Success(val newItems: List<StorageItem>, val hasMore: Boolean) : LoadMoreResult()
    data class Error(val message: String) : LoadMoreResult()
}

/**
 * Encapsulates loading of folder items, recent/starred/trash lists, breadcrumbs, and pagination.
 * Keeps [FilesViewModel] focused on state and UI callbacks.
 */
internal class FilesLoader(
    private val getFolderItemsUseCase: GetFolderItemsUseCase,
    private val getRecentUseCase: GetRecentUseCase,
    private val getStarredUseCase: GetStarredUseCase,
    private val getTrashUseCase: GetTrashUseCase,
    private val getBreadcrumbsUseCase: GetBreadcrumbsUseCase,
    private val folderPageSize: Int,
) {

    suspend fun loadItemsForMode(
        mode: MainComponent.FilesMode,
        currentFolderId: String?,
        sortField: SortField,
        sortOrder: SortOrder,
        currentFolderName: String?,
    ): LoadItemsResult {
        return when (mode) {
            MainComponent.FilesMode.ALL -> loadFolderItems(
                currentFolderId = currentFolderId,
                sortField = sortField,
                sortOrder = sortOrder,
                offset = 0,
                currentFolderName = currentFolderName,
            )
            MainComponent.FilesMode.RECENT, MainComponent.FilesMode.STARRED, MainComponent.FilesMode.TRASH -> {
                val listResult = when (mode) {
                    MainComponent.FilesMode.RECENT -> getRecentUseCase()
                    MainComponent.FilesMode.STARRED -> getStarredUseCase()
                    MainComponent.FilesMode.TRASH -> getTrashUseCase()
                    MainComponent.FilesMode.ALL -> getRecentUseCase()
                }
                when (listResult) {
                    is Result.Success -> LoadItemsResult.Success(
                        items = listResult.data,
                        total = listResult.data.size.toLong(),
                        hasMore = false,
                        breadcrumbs = listOf(Breadcrumb(id = null, name = "Home", path = "/")),
                        currentFolderName = null,
                    )
                    is Result.Error -> LoadItemsResult.Error(listResult.message)
                    is Result.NetworkError -> LoadItemsResult.Error(listResult.message)
                }
            }
        }
    }

    private suspend fun loadFolderItems(
        currentFolderId: String?,
        sortField: SortField,
        sortOrder: SortOrder,
        offset: Int,
        currentFolderName: String?,
    ): LoadItemsResult {
        val folderResult = getFolderItemsUseCase(
            folderId = currentFolderId,
            sortBy = sortField,
            sortOrder = sortOrder,
            limit = folderPageSize,
            offset = offset,
        )
        return when (folderResult) {
            is Result.Success -> {
                val data = folderResult.data
                val breadcrumbs = if (currentFolderId != null) {
                    when (val bc = getBreadcrumbsUseCase(currentFolderId)) {
                        is Result.Success -> {
                            val home = Breadcrumb(id = null, name = "Home", path = "/")
                            val current = currentFolderName?.let {
                                listOf(Breadcrumb(id = currentFolderId, name = it, path = "/"))
                            } ?: emptyList()
                            listOf(home) + bc.data + current
                        }
                        else -> listOf(
                            Breadcrumb(id = null, name = "Home", path = "/"),
                            Breadcrumb(id = currentFolderId, name = currentFolderName ?: "", path = "/"),
                        )
                    }
                } else {
                    listOf(Breadcrumb(id = null, name = "Home", path = "/"))
                }
                LoadItemsResult.Success(
                    items = data.items,
                    total = data.total,
                    hasMore = data.hasMore,
                    breadcrumbs = breadcrumbs,
                    currentFolderName = if (currentFolderId != null) currentFolderName else null,
                )
            }
            is Result.Error -> LoadItemsResult.Error(folderResult.message)
            is Result.NetworkError -> LoadItemsResult.Error(folderResult.message)
        }
    }

    suspend fun loadMoreFolderItems(
        currentFolderId: String?,
        sortField: SortField,
        sortOrder: SortOrder,
        currentOffset: Int,
    ): LoadMoreResult {
        val result = getFolderItemsUseCase(
            folderId = currentFolderId,
            sortBy = sortField,
            sortOrder = sortOrder,
            limit = folderPageSize,
            offset = currentOffset,
        )
        return when (result) {
            is Result.Success -> LoadMoreResult.Success(
                newItems = result.data.items,
                hasMore = result.data.hasMore,
            )
            is Result.Error -> LoadMoreResult.Error(result.message)
            is Result.NetworkError -> LoadMoreResult.Error(result.message)
        }
    }

    suspend fun loadBreadcrumbs(
        folderId: String,
        currentFolderName: String?,
    ): List<Breadcrumb> {
        return when (val result = getBreadcrumbsUseCase(folderId)) {
            is Result.Success -> {
                val home = Breadcrumb(id = null, name = "Home", path = "/")
                val current = currentFolderName?.let {
                    listOf(Breadcrumb(id = folderId, name = it, path = "/"))
                } ?: emptyList()
                listOf(home) + result.data + current
            }
            else -> emptyList()
        }
    }

    suspend fun resolvePathSegments(
        segments: List<String>,
        sortField: SortField,
        sortOrder: SortOrder,
    ): Pair<String?, String?>? {
        if (segments.isEmpty()) return null
        var parentId: String? = null
        var lastName: String? = null
        for (segment in segments) {
            val result = getFolderItemsUseCase(
                folderId = parentId,
                sortBy = sortField,
                sortOrder = sortOrder,
                limit = folderPageSize,
                offset = 0,
            )
            when (result) {
                is Result.Success -> {
                    val folder = result.data.items.find { it.name == segment && it.isFolder }
                        ?: return parentId to lastName
                    parentId = folder.id
                    lastName = segment
                }
                else -> return parentId to lastName
            }
        }
        return parentId to lastName
    }
}
