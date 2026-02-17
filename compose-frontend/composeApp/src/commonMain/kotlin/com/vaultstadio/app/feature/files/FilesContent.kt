package com.vaultstadio.app.feature.files

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.StorageItem
import com.vaultstadio.app.domain.model.ViewMode
import com.vaultstadio.app.domain.upload.FolderUploadEntry
import com.vaultstadio.app.domain.upload.UploadQueueEntry
import com.vaultstadio.app.feature.main.MainComponent
import com.vaultstadio.app.feature.upload.LocalUploadManager
import com.vaultstadio.app.i18n.LocalStrings
import com.vaultstadio.app.i18n.files
import com.vaultstadio.app.i18n.newFolder
import com.vaultstadio.app.i18n.noFiles
import com.vaultstadio.app.i18n.noRecentFiles
import com.vaultstadio.app.i18n.noStarredFiles
import com.vaultstadio.app.i18n.recent
import com.vaultstadio.app.i18n.starred
import com.vaultstadio.app.i18n.trash
import com.vaultstadio.app.i18n.trashEmpty
import com.vaultstadio.app.navigation.MainDestination
import com.vaultstadio.app.navigation.RoutePaths
import com.vaultstadio.app.platform.openFilePicker
import com.vaultstadio.app.platform.openFolderPicker
import com.vaultstadio.app.platform.pickLargeFilesForUpload
import com.vaultstadio.app.platform.setPath
import com.vaultstadio.app.ui.components.files.FileInfoPanel
import com.vaultstadio.app.ui.components.layout.SelectionToolbar
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Files screen content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesContent(
    component: FilesComponent,
    modifier: Modifier = Modifier,
) {
    val viewModel: FilesViewModel = koinViewModel { parametersOf(component.mode) }
    val strings = LocalStrings.current

    // When this screen (Files / Recent / Starred / Trash) is shown, load data for that mode.
    // This ensures the correct API is called and content is shown even if the same ViewModel
    // instance is reused when switching between sidebar destinations.
    LaunchedEffect(component.mode) {
        viewModel.loadItemsForMode(component.mode)
    }
    val snackbarHostState = remember { SnackbarHostState() }

    val uploadManager = LocalUploadManager.current

    var dialogState by remember { mutableStateOf(FilesScreenDialogState()) }
    var showUploadMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Drag-and-drop: move item to folder or parent
    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var totalDragOffset by remember { mutableStateOf(Offset.Zero) }
    var dropTargetUnderPointer by remember { mutableStateOf<String?>(null) }
    var dragOverlayPosition by remember { mutableStateOf<Offset?>(null) }
    var contentBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    val itemBounds = remember { mutableMapOf<String, Rect>() }
    val dropTargetBounds = remember { mutableMapOf<String, Rect>() }

    // Context menu (right-click) on file/folder
    var contextMenuForId by remember { mutableStateOf<String?>(null) }

    // Show error in snackbar
    LaunchedEffect(viewModel.error) {
        viewModel.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Resolve URL path segments on load (e.g. /files/folderA/folderB opens that folder)
    LaunchedEffect(component.pathSegments) {
        if (component.pathSegments.isNotEmpty()) {
            viewModel.openPathFromSegments(component.pathSegments)
        }
    }

    // Sync browser URL with current folder path when in Files mode (e.g. /files/test_folder/other_folder)
    LaunchedEffect(viewModel.breadcrumbs, component.mode) {
        if (component.mode == MainComponent.FilesMode.ALL) {
            val segments = viewModel.breadcrumbs
                .filter { it.id != null }
                .map { it.name }
            val pathParams = if (segments.isEmpty()) null else mapOf("path" to segments)
            setPath(RoutePaths.toPath(MainDestination.FILES, pathParams))
        }
    }

    // Tell upload manager where to upload/drop when user is in a folder (so files go into current folder)
    LaunchedEffect(component.mode, viewModel.currentFolderId, uploadManager) {
        uploadManager?.let {
            if (component.mode == MainComponent.FilesMode.ALL) {
                it.setUploadDestination(viewModel.currentFolderId)
            } else {
                it.setUploadDestination(null)
            }
        }
    }

    // Refresh file list when an upload completes so the new file appears without manual refresh
    LaunchedEffect(uploadManager, component.mode) {
        uploadManager ?: return@LaunchedEffect
        uploadManager.uploadCompleted.collect {
            if (component.mode == MainComponent.FilesMode.ALL) {
                viewModel.loadItemsForMode(component.mode)
            }
        }
    }

    var uploadAction by remember { mutableStateOf<UploadAction?>(null) }
    LaunchedEffect(uploadAction) {
        val manager = uploadManager ?: return@LaunchedEffect
        val action = uploadAction ?: return@LaunchedEffect
        val parentId = viewModel.currentFolderId
        when (action) {
            UploadAction.Files -> {
                val files = openFilePicker(multiple = true, accept = "*/*")
                val entries = files
                    .filter { it.data.isNotEmpty() }
                    .map { f -> UploadQueueEntry.WithData(f.name, f.size, f.mimeType, f.data) }
                if (entries.isNotEmpty()) manager.addEntries(entries, parentId)
            }
            UploadAction.Folder -> {
                val folderFiles = openFolderPicker()
                val entries = folderFiles.map { f ->
                    FolderUploadEntry(f.name, f.relativePath, f.size, f.mimeType, f.data)
                }
                if (entries.isNotEmpty()) manager.addFolderEntries(entries, parentId)
            }
            UploadAction.LargeFiles -> {
                val entries = pickLargeFilesForUpload()
                if (entries.isNotEmpty()) manager.addEntries(entries, parentId)
            }
        }
        uploadAction = null
    }

    val title = when (component.mode) {
        MainComponent.FilesMode.ALL -> strings.files
        MainComponent.FilesMode.RECENT -> strings.recent
        MainComponent.FilesMode.STARRED -> strings.starred
        MainComponent.FilesMode.TRASH -> strings.trash
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            FilesTopBar(
                title = title,
                showBackButton = component.mode == MainComponent.FilesMode.ALL &&
                    viewModel.breadcrumbs.size > 1,
                onNavigateUp = { viewModel.navigateUp() },
                searchQuery = viewModel.searchQuery,
                onClearSearch = { viewModel.clearSearch() },
                showSortMenu = showSortMenu,
                onSortMenuChange = { showSortMenu = it },
                sortField = viewModel.sortField,
                sortOrder = viewModel.sortOrder,
                onSortField = { viewModel.updateSortField(it) },
                onSortOrder = { viewModel.updateSortOrder(it) },
                viewMode = viewModel.viewMode,
                onViewModeToggle = {
                    viewModel.updateViewMode(
                        if (viewModel.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID,
                    )
                },
                hasItems = viewModel.items.isNotEmpty(),
                onSelectAll = { viewModel.selectAll() },
                onRefresh = { viewModel.refresh() },
                showUploadButton = component.mode == MainComponent.FilesMode.ALL &&
                    uploadManager != null,
                showUploadMenu = showUploadMenu,
                onUploadMenuChange = { showUploadMenu = it },
                onUploadAction = { uploadAction = it },
                showEmptyTrashButton = component.mode == MainComponent.FilesMode.TRASH &&
                    viewModel.items.isNotEmpty(),
                onEmptyTrash = { viewModel.emptyTrash(component.mode) },
                strings = strings,
            )
        },
        floatingActionButton = {
            if (component.mode == MainComponent.FilesMode.ALL && !viewModel.isSelectionMode) {
                FloatingActionButton(onClick = {
                    dialogState = dialogState.copy(showNewFolderDialog = true)
                }) {
                    Icon(Icons.Default.Add, contentDescription = strings.newFolder)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Main content
            Column(
                modifier = Modifier.weight(1f),
            ) {
                if (component.mode == MainComponent.FilesMode.ALL) {
                    FilesBreadcrumbsBar(
                        breadcrumbs = viewModel.breadcrumbs,
                        currentFolderId = viewModel.currentFolderId,
                        onNavigate = { viewModel.navigateToFolder(it.id, it.name) },
                        dropTargetBounds = dropTargetBounds,
                        dropTargetUnderPointer = dropTargetUnderPointer,
                        draggedItemId = draggedItemId,
                    )
                }

                FilesSearchBar(
                    searchQuery = viewModel.searchQuery,
                    onQueryChange = { query ->
                        viewModel.search(query)
                        if (query.length >= 2) {
                            viewModel.loadSearchSuggestions(query)
                        } else {
                            viewModel.clearSuggestions()
                        }
                    },
                    onClearSearch = { viewModel.clearSearch() },
                    suggestions = viewModel.searchSuggestions,
                    onSuggestionClick = {
                        viewModel.search(it)
                        viewModel.clearSuggestions()
                    },
                    onClearSuggestions = { viewModel.clearSuggestions() },
                    isSearching = viewModel.isSearching,
                    strings = strings,
                )

                // Selection toolbar
                if (viewModel.isSelectionMode) {
                    SelectionToolbar(
                        selectedCount = viewModel.selectedItems.size,
                        onDelete = { viewModel.batchDelete(component.mode == MainComponent.FilesMode.TRASH) },
                        onClearSelection = { viewModel.clearSelection() },
                        showMoveAndCopy = component.mode == MainComponent.FilesMode.ALL,
                        onMove = if (component.mode == MainComponent.FilesMode.ALL) {
                            { dialogState = dialogState.copy(showMoveDialog = true) }
                        } else {
                            null
                        },
                        onCopy = if (component.mode == MainComponent.FilesMode.ALL) {
                            { dialogState = dialogState.copy(showCopyDialog = true) }
                        } else {
                            null
                        },
                        onStar = if (component.mode != MainComponent.FilesMode.TRASH) {
                            { viewModel.batchStar(true) }
                        } else {
                            null
                        },
                        onDownloadZip = if (component.mode != MainComponent.FilesMode.TRASH) {
                            {
                                // Get the ZIP download URL for selected items
                                val zipUrl = viewModel.getDownloadZipUrl()
                                // Platform-specific download handling
                            }
                        } else {
                            null
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                val emptyTitle = when (component.mode) {
                    MainComponent.FilesMode.ALL -> strings.noFiles
                    MainComponent.FilesMode.RECENT -> strings.noRecentFiles
                    MainComponent.FilesMode.STARRED -> strings.noStarredFiles
                    MainComponent.FilesMode.TRASH -> strings.trashEmpty
                }
                val gridCallbacks = remember {
                    object : FilesGridContextMenuCallbacks {
                        override fun onRequestRename(item: StorageItem) {
                            dialogState = dialogState.copy(renameValue = item.name, showRenameDialog = true)
                            viewModel.showItemInfo(item)
                        }
                        override fun onRequestMove(item: StorageItem) {
                            viewModel.clearSelection()
                            viewModel.toggleItemSelection(item.id)
                            dialogState = dialogState.copy(showMoveDialog = true)
                        }
                        override fun onRequestCopy(item: StorageItem) {
                            viewModel.clearSelection()
                            viewModel.toggleItemSelection(item.id)
                            dialogState = dialogState.copy(showCopyDialog = true)
                        }
                        override fun onRequestRestore(itemId: String) {
                            dialogState = dialogState.copy(itemToRestore = itemId)
                        }
                        override fun onRequestDeletePermanently(itemId: String) {
                            dialogState = dialogState.copy(itemToDeletePermanently = itemId)
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    if (viewModel.viewMode == ViewMode.LIST) {
                        FilesListContent(
                            viewModel = viewModel,
                            mode = component.mode,
                            emptyTitle = emptyTitle,
                            isLoading = viewModel.isLoading,
                            items = viewModel.items,
                            folderHasMore = component.mode == MainComponent.FilesMode.ALL && viewModel.folderHasMore,
                            onLoadMore = { viewModel.loadMoreFolderItems() },
                            contextMenuForId = contextMenuForId,
                            onContextMenuForId = { contextMenuForId = it },
                            callbacks = gridCallbacks,
                        )
                    } else {
                        FilesGridContent(
                            viewModel = viewModel,
                            mode = component.mode,
                            emptyTitle = emptyTitle,
                            isLoading = viewModel.isLoading,
                            items = viewModel.items,
                            folderHasMore = component.mode == MainComponent.FilesMode.ALL && viewModel.folderHasMore,
                            onLoadMore = { viewModel.loadMoreFolderItems() },
                            contextMenuForId = contextMenuForId,
                            onContextMenuForId = { contextMenuForId = it },
                            draggedItemId = draggedItemId,
                            onDraggedItemId = { draggedItemId = it },
                            totalDragOffset = totalDragOffset,
                            onTotalDragOffset = { totalDragOffset = it },
                            dropTargetUnderPointer = dropTargetUnderPointer,
                            onDropTargetUnderPointer = { dropTargetUnderPointer = it },
                            dragOverlayPosition = dragOverlayPosition,
                            onDragOverlayPosition = { dragOverlayPosition = it },
                            contentBoundsInRoot = contentBoundsInRoot,
                            onContentBoundsInRoot = { contentBoundsInRoot = it },
                            itemBounds = itemBounds,
                            dropTargetBounds = dropTargetBounds,
                            callbacks = gridCallbacks,
                        )
                    }
                }
            }

            // Info panel: use item from list when available so starred/updated state is always in sync
            if (viewModel.showInfoPanel && viewModel.selectedInfoItem != null) {
                val infoPanelItem = viewModel.selectedInfoItem!!
                val currentItem =
                    viewModel.items.find { it.id == infoPanelItem.id } ?: infoPanelItem
                FileInfoPanel(
                    item = currentItem,
                    itemActivity = viewModel.itemActivity,
                    isLoadingActivity = viewModel.isLoadingActivity,
                    isTrashMode = component.mode == MainComponent.FilesMode.TRASH,
                    previewUrl = viewModel.selectedInfoItem?.let {
                        if (!it.isFolder) viewModel.getPreviewUrl(it.id) else null
                    },
                    onClose = { viewModel.hideItemInfo() },
                    onRename = {
                        dialogState = dialogState.copy(
                            renameValue = viewModel.selectedInfoItem?.name ?: "",
                            showRenameDialog = true,
                        )
                    },
                    onMove = { dialogState = dialogState.copy(showMoveDialog = true) },
                    onCopy = { dialogState = dialogState.copy(showCopyDialog = true) },
                    onShare = { dialogState = dialogState.copy(showShareDialog = true) },
                    onDownload = {
                        viewModel.selectedInfoItem?.let { item ->
                            val url = viewModel.getDownloadUrl(item.id)
                            // Platform-specific download handling will open this URL
                            // For web, this triggers browser download
                            // For desktop/mobile, handled via platform code
                        }
                    },
                    onStar = { viewModel.toggleStar(currentItem) },
                    onDelete = { viewModel.trashItem(viewModel.selectedInfoItem!!.id) },
                    onRestore = if (component.mode == MainComponent.FilesMode.TRASH) {
                        { dialogState = dialogState.copy(itemToRestore = viewModel.selectedInfoItem!!.id) }
                    } else {
                        null
                    },
                    onDeletePermanently = if (component.mode == MainComponent.FilesMode.TRASH) {
                        { dialogState = dialogState.copy(itemToDeletePermanently = viewModel.selectedInfoItem!!.id) }
                    } else {
                        null
                    },
                    onPreview = viewModel.selectedInfoItem?.let { item ->
                        if (!item.isFolder) {
                            {
                                val previewUrl = viewModel.getPreviewUrl(item.id)
                                // Platform-specific preview handling
                            }
                        } else {
                            null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.3f),
                )
            }
        }
    }

    FilesScreenDialogs(
        state = dialogState,
        onStateChange = { dialogState = it },
        viewModel = viewModel,
        mode = component.mode,
        strings = strings,
    )
}
