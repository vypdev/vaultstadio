/**
 * Single grid item with drag-and-drop and context menu for the Files grid.
 * Extracted from FilesGridContent to keep the main file under the line limit.
 */

package com.vaultstadio.app.feature.files

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.storage.model.ItemType
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.ui.components.files.FileGridItem
import com.vaultstadio.app.ui.components.files.SelectableFileGridItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FilesGridItemWithDrag(
    item: StorageItem,
    viewModel: FilesViewModel,
    mode: FilesMode,
    contextMenuForId: String?,
    onContextMenuForId: (String?) -> Unit,
    draggedItemId: String?,
    onDraggedItemId: (String?) -> Unit,
    totalDragOffset: Offset,
    onTotalDragOffset: (Offset) -> Unit,
    dropTargetUnderPointer: String?,
    onDropTargetUnderPointer: (String?) -> Unit,
    onDragOverlayPosition: (Offset?) -> Unit,
    itemBounds: MutableMap<String, Rect>,
    dropTargetBounds: MutableMap<String, Rect>,
    callbacks: FilesGridContextMenuCallbacks,
) {
    val isFolder = item.type == ItemType.FOLDER
    val canDrag = mode == FilesMode.ALL && !viewModel.isSelectionMode
    val isDragging = draggedItemId == item.id
    val isDropTargetHighlight = isFolder &&
        dropTargetUnderPointer == item.id &&
        draggedItemId != null &&
        draggedItemId != item.id

    Box(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInRoot()
                itemBounds[item.id] = bounds
                if (isFolder) {
                    dropTargetBounds[item.id] = bounds
                } else {
                    dropTargetBounds.remove(item.id)
                }
            }
            .then(
                if (canDrag) {
                    Modifier.pointerInput(item.id) {
                        var accumulatedOffset = Offset.Zero
                        var dragStartBounds: Rect? = null
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragStartBounds = itemBounds[item.id]
                                accumulatedOffset = offset
                                onDraggedItemId(item.id)
                                onTotalDragOffset(offset)
                                onDropTargetUnderPointer(null)
                                onDragOverlayPosition(dragStartBounds?.topLeft?.plus(offset))
                            },
                            onDrag = { _, dragAmount ->
                                accumulatedOffset += dragAmount
                                val pos = dragStartBounds?.topLeft?.plus(accumulatedOffset)
                                if (pos != null) {
                                    onDragOverlayPosition(pos)
                                    onDropTargetUnderPointer(
                                        dropTargetBounds.entries
                                            .firstOrNull { (key, rect) ->
                                                key != item.id && rect.contains(pos)
                                            }
                                            ?.key,
                                    )
                                }
                            },
                            onDragEnd = {
                                val releasePos = dragStartBounds?.topLeft?.plus(accumulatedOffset)
                                val targetKey = releasePos?.let { pos ->
                                    dropTargetBounds.entries
                                        .firstOrNull { (key, rect) ->
                                            key != item.id && rect.contains(pos)
                                        }
                                        ?.key
                                }
                                targetKey?.let { key ->
                                    val destId = if (key == "parent") {
                                        viewModel.breadcrumbs
                                            .getOrNull(viewModel.breadcrumbs.size - 2)
                                            ?.id
                                    } else {
                                        key
                                    }
                                    viewModel.moveItemToFolder(item.id, destId)
                                }
                                onDraggedItemId(null)
                                onTotalDragOffset(Offset.Zero)
                                onDropTargetUnderPointer(null)
                                onDragOverlayPosition(null)
                            },
                            onDragCancel = {
                                onDraggedItemId(null)
                                onTotalDragOffset(Offset.Zero)
                                onDropTargetUnderPointer(null)
                                onDragOverlayPosition(null)
                            },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .then(
                if (isDropTargetHighlight) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    Modifier
                },
            ),
    ) {
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
                    onRestore = if (mode == FilesMode.TRASH) {
                        {
                            onContextMenuForId(null)
                            callbacks.onRequestRestore(item.id)
                        }
                    } else {
                        null
                    },
                    onDeletePermanently = if (mode == FilesMode.TRASH) {
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
        if (viewModel.isSelectionMode) {
            SelectableFileGridItem(
                item = item,
                isSelected = item.id in viewModel.selectedItems,
                onToggleSelection = { viewModel.toggleItemSelection(item.id) },
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
                thumbnailUrl = if (item.mimeType?.startsWith("image/") == true) {
                    viewModel.getThumbnailUrl(item.id)
                } else {
                    null
                },
                modifier = Modifier.then(
                    if (isDragging) Modifier.alpha(0.5f) else Modifier,
                ),
            )
        } else {
            FileGridItem(
                item = item,
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
                thumbnailUrl = if (item.mimeType?.startsWith("image/") == true) {
                    viewModel.getThumbnailUrl(item.id)
                } else {
                    null
                },
                modifier = Modifier.then(
                    if (isDragging) Modifier.alpha(0.5f) else Modifier,
                ),
            )
        }
    }
}
