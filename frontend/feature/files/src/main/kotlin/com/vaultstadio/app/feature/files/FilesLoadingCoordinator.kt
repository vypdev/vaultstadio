package com.vaultstadio.app.feature.files

import com.vaultstadio.app.domain.storage.model.SortField
import com.vaultstadio.app.domain.storage.model.SortOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal data class FilesLoadContext(
    val mode: FilesMode,
    val folderId: String?,
    val folderName: String?,
    val sort: SortField,
    val order: SortOrder,
)

/** Own request lifetimes separately from UI state; even an uncooperative port cannot publish stale data. */
internal class FilesLoadingCoordinator(private val scope: CoroutineScope, private val loader: FilesLoader) {
    private var revision = 0L
    private var loadJob: Job? = null
    private var pageJob: Job? = null

    fun load(
        context: FilesLoadContext,
        started: () -> Unit,
        completed: (LoadItemsResult) -> Unit,
        finished: () -> Unit,
    ) {
        val identity = ++revision
        loadJob?.cancel()
        pageJob?.cancel()
        loadJob = scope.launch {
            started()
            try {
                val result = loader.loadItemsForMode(context.mode, context.folderId, context.sort, context.order, context.folderName)
                if (identity == revision) completed(result)
            } finally {
                if (identity == revision) finished()
            }
        }
    }

    fun more(
        context: FilesLoadContext,
        offset: Int,
        started: () -> Unit,
        completed: (LoadMoreResult) -> Unit,
        finished: () -> Unit,
    ) {
        if (pageJob?.isActive == true) return
        val identity = revision
        pageJob = scope.launch {
            started()
            try {
                val result = loader.loadMoreFolderItems(context.folderId, context.sort, context.order, offset)
                if (identity == revision) completed(result)
            } finally {
                if (identity == revision) finished()
            }
        }
    }
}
