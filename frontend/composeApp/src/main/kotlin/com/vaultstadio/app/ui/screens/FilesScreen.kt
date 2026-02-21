/**
 * VaultStadio Files Screen
 */

package com.vaultstadio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.domain.storage.model.ViewMode
import com.vaultstadio.app.ui.components.files.Breadcrumbs
import com.vaultstadio.app.ui.components.files.EmptyState
import com.vaultstadio.app.ui.components.files.FileGridItem
import com.vaultstadio.app.ui.components.files.FileListItem

/**
 * Files screen displaying folder contents.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    items: List<StorageItem>,
    breadcrumbs: List<StorageItem>,
    currentFolder: StorageItem?,
    isLoading: Boolean,
    viewMode: ViewMode,
    onNavigateToFolder: (String?) -> Unit,
    onItemClick: (StorageItem) -> Unit,
    onStarClick: (StorageItem) -> Unit,
    onMenuClick: (StorageItem) -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onSearch: (String) -> Unit,
    onCreateFolder: () -> Unit,
    onUpload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        // Toolbar
        TopAppBar(
            title = {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.length >= 2) onSearch(it)
                    },
                    placeholder = { Text("Search files...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear",
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            actions = {
                // View mode toggle
                IconButton(onClick = {
                    onViewModeChange(
                        if (viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID,
                    )
                }) {
                    Icon(
                        imageVector = if (viewMode ==
                            ViewMode.GRID
                        ) {
                            Icons.AutoMirrored.Filled.ViewList
                        } else {
                            Icons.Filled.ViewModule
                        },
                        contentDescription = "Toggle view",
                    )
                }

                // Create folder
                IconButton(onClick = onCreateFolder) {
                    Icon(
                        imageVector = Icons.Filled.CreateNewFolder,
                        contentDescription = "Create folder",
                    )
                }

                // Upload
                IconButton(onClick = onUpload) {
                    Icon(
                        imageVector = Icons.Filled.Upload,
                        contentDescription = "Upload",
                    )
                }
            },
        )

        // Breadcrumbs
        Breadcrumbs(
            items = breadcrumbs,
            currentFolderName = currentFolder?.name,
            onNavigate = onNavigateToFolder,
        )

        HorizontalDivider()

        // Content
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }
                items.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Filled.Folder,
                        title = "This folder is empty",
                        description = "Upload files or create a folder to get started",
                        actionLabel = "Upload",
                        onAction = onUpload,
                    )
                }
                else -> {
                    if (viewMode == ViewMode.GRID) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(items, key = { it.id }) { item ->
                                FileGridItem(
                                    item = item,
                                    onClick = { onItemClick(item) },
                                    onStarClick = { onStarClick(item) },
                                    onMenuClick = { onMenuClick(item) },
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(items, key = { it.id }) { item ->
                                FileListItem(
                                    item = item,
                                    onClick = { onItemClick(item) },
                                    onStarClick = { onStarClick(item) },
                                    onMenuClick = { onMenuClick(item) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
