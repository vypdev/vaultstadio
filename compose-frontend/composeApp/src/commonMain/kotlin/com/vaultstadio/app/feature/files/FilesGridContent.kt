/**
 * Grid content for Files screen: loading / empty / LazyVerticalGrid with drag-and-drop and context menu.
 */

package com.vaultstadio.app.feature.files

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.StorageItem
import com.vaultstadio.app.feature.main.MainComponent
import com.vaultstadio.app.ui.components.files.EmptyState
import kotlin.math.roundToInt

/**
 * Callbacks for context menu actions that require opening a dialog (handled by FilesContent).
 */
interface FilesGridContextMenuCallbacks {
    fun onRequestRename(item: StorageItem)
    fun onRequestMove(item: StorageItem)
    fun onRequestCopy(item: StorageItem)
    fun onRequestRestore(itemId: String)
    fun onRequestDeletePermanently(itemId: String)
}

@Composable
internal fun FilesGridContent(
    viewModel: FilesViewModel,
    mode: MainComponent.FilesMode,
    emptyTitle: String,
    isLoading: Boolean,
    items: List<StorageItem>,
    folderHasMore: Boolean,
    onLoadMore: () -> Unit,
    contextMenuForId: String?,
    onContextMenuForId: (String?) -> Unit,
    draggedItemId: String?,
    onDraggedItemId: (String?) -> Unit,
    totalDragOffset: Offset,
    onTotalDragOffset: (Offset) -> Unit,
    dropTargetUnderPointer: String?,
    onDropTargetUnderPointer: (String?) -> Unit,
    dragOverlayPosition: Offset?,
    onDragOverlayPosition: (Offset?) -> Unit,
    contentBoundsInRoot: Rect?,
    onContentBoundsInRoot: (Rect?) -> Unit,
    itemBounds: MutableMap<String, Rect>,
    dropTargetBounds: MutableMap<String, Rect>,
    callbacks: FilesGridContextMenuCallbacks,
) {
    val gridState = rememberLazyGridState()
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            folderHasMore && totalItems > 0 && lastVisibleIndex >= totalItems - 6
        }
    }
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) onLoadMore()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { onContentBoundsInRoot(it.boundsInRoot()) },
    ) {
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
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(180.dp),
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        FilesGridItemWithDrag(
                            item = item,
                            viewModel = viewModel,
                            mode = mode,
                            contextMenuForId = contextMenuForId,
                            onContextMenuForId = onContextMenuForId,
                            draggedItemId = draggedItemId,
                            onDraggedItemId = onDraggedItemId,
                            totalDragOffset = totalDragOffset,
                            onTotalDragOffset = onTotalDragOffset,
                            dropTargetUnderPointer = dropTargetUnderPointer,
                            onDropTargetUnderPointer = onDropTargetUnderPointer,
                            onDragOverlayPosition = onDragOverlayPosition,
                            itemBounds = itemBounds,
                            dropTargetBounds = dropTargetBounds,
                            callbacks = callbacks,
                        )
                    }
                }
            }
        }

        if (draggedItemId != null && dragOverlayPosition != null && contentBoundsInRoot != null) {
            val draggedItem = items.find { it.id == draggedItemId }
            if (draggedItem != null) {
                val relX = dragOverlayPosition.x - contentBoundsInRoot.left
                val relY = dragOverlayPosition.y - contentBoundsInRoot.top
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset { IntOffset(relX.roundToInt(), relY.roundToInt()) }
                        .offset((-80).dp, (-80).dp),
                ) {
                    DragPreviewCard(item = draggedItem)
                }
            }
        }
    }
}
