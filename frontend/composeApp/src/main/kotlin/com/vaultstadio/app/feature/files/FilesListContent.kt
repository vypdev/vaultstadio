/**
 * List content for Files screen: loading / empty / LazyColumn with context menu and selection.
 */

package com.vaultstadio.app.feature.files

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.storage.model.ItemType
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.feature.main.MainComponent
import com.vaultstadio.app.ui.components.files.EmptyState
import com.vaultstadio.app.ui.components.files.SelectableFileListItem

@Composable
internal fun FilesListContent(
    viewModel: FilesViewModel,
    mode: MainComponent.FilesMode,
    emptyTitle: String,
    isLoading: Boolean,
    items: List<StorageItem>,
    folderHasMore: Boolean,
    onLoadMore: () -> Unit,
    contextMenuForId: String?,
    onContextMenuForId: (String?) -> Unit,
    callbacks: FilesGridContextMenuCallbacks,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            folderHasMore && totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
    }
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) onLoadMore()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading && items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(title = emptyTitle)
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        FilesListItem(
                            item = item,
                            viewModel = viewModel,
                            mode = mode,
                            contextMenuForId = contextMenuForId,
                            onContextMenuForId = onContextMenuForId,
                            callbacks = callbacks,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FilesListItem(
    item: StorageItem,
    viewModel: FilesViewModel,
    mode: MainComponent.FilesMode,
    contextMenuForId: String?,
    onContextMenuForId: (String?) -> Unit,
    callbacks: FilesGridContextMenuCallbacks,
) {
    Box(
        modifier = Modifier.combinedClickable(
            onClick = { /* handled by SelectableFileListItem */ },
            onLongClick = {
                onContextMenuForId(item.id)
                viewModel.toggleItemSelection(item.id)
            },
        ),
    ) {
        SelectableFileListItem(
            item = item,
            isSelected = item.id in viewModel.selectedItems,
            isSelectionMode = viewModel.isSelectionMode,
            onClick = {
                if (item.type == ItemType.FOLDER) {
                    viewModel.navigateToFolder(item.id, item.name)
                } else {
                    viewModel.showItemInfo(item)
                }
            },
            onLongClick = {
                onContextMenuForId(item.id)
                viewModel.toggleItemSelection(item.id)
            },
            onStarClick = { viewModel.toggleStar(item) },
            onMenuClick = {
                onContextMenuForId(item.id)
                viewModel.toggleItemSelection(item.id)
            },
        )
        if (contextMenuForId == item.id) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = { onContextMenuForId(null) },
            ) {
                FileContextMenuItems(
                    item = item,
                    mode = mode,
                    onOpen = {
                        onContextMenuForId(null)
                        if (item.type == ItemType.FOLDER) {
                            viewModel.navigateToFolder(item.id, item.name)
                        } else {
                            viewModel.showItemInfo(item)
                        }
                    },
                    onRename = {
                        onContextMenuForId(null)
                        callbacks.onRequestRename(item)
                    },
                    onMove = {
                        onContextMenuForId(null)
                        viewModel.clearSelection()
                        viewModel.toggleItemSelection(item.id)
                        callbacks.onRequestMove(item)
                    },
                    onCopy = {
                        onContextMenuForId(null)
                        viewModel.clearSelection()
                        viewModel.toggleItemSelection(item.id)
                        callbacks.onRequestCopy(item)
                    },
                    onDownload = {
                        onContextMenuForId(null)
                        viewModel.getDownloadUrl(item.id)
                    },
                    onStar = {
                        onContextMenuForId(null)
                        viewModel.toggleStar(item)
                    },
                    onDelete = {
                        onContextMenuForId(null)
                        viewModel.trashItem(item.id)
                    },
                    onRestore = if (mode == MainComponent.FilesMode.TRASH) {
                        {
                            onContextMenuForId(null)
                            callbacks.onRequestRestore(item.id)
                        }
                    } else {
                        null
                    },
                    onDeletePermanently = if (mode == MainComponent.FilesMode.TRASH) {
                        {
                            onContextMenuForId(null)
                            callbacks.onRequestDeletePermanently(item.id)
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
