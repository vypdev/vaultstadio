/**
 * Files screen top app bar: title, navigation, sort, view mode, upload, etc.
 */

package com.vaultstadio.app.feature.files

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.core.resources.StringResources
import com.vaultstadio.app.core.resources.clear
import com.vaultstadio.app.core.resources.emptyTrash
import com.vaultstadio.app.core.resources.refresh
import com.vaultstadio.app.core.resources.selectAll
import com.vaultstadio.app.core.resources.toggleView
import com.vaultstadio.app.domain.storage.model.SortField
import com.vaultstadio.app.domain.storage.model.SortOrder
import com.vaultstadio.app.domain.storage.model.ViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilesTopBar(
    title: String,
    showBackButton: Boolean,
    onNavigateUp: () -> Unit,
    searchQuery: String,
    onClearSearch: () -> Unit,
    showSortMenu: Boolean,
    onSortMenuChange: (Boolean) -> Unit,
    sortField: SortField,
    sortOrder: SortOrder,
    onSortField: (SortField) -> Unit,
    onSortOrder: (SortOrder) -> Unit,
    viewMode: ViewMode,
    onViewModeToggle: () -> Unit,
    hasItems: Boolean,
    onSelectAll: () -> Unit,
    onRefresh: () -> Unit,
    showUploadButton: Boolean,
    showUploadMenu: Boolean,
    onUploadMenuChange: (Boolean) -> Unit,
    onUploadAction: (UploadAction) -> Unit,
    showEmptyTrashButton: Boolean,
    onEmptyTrash: () -> Unit,
    strings: StringResources,
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onNavigateUp) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Up")
                }
            }
        },
        actions = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = onClearSearch) {
                    Icon(Icons.Default.Close, contentDescription = strings.clear)
                }
            }

            BoxWithSortMenu(
                showSortMenu = showSortMenu,
                onDismissRequest = { onSortMenuChange(false) },
                sortField = sortField,
                sortOrder = sortOrder,
                onSortField = onSortField,
                onSortOrder = onSortOrder,
                onSortMenuChange = onSortMenuChange,
            )

            IconButton(onClick = onViewModeToggle) {
                Icon(
                    if (viewMode == ViewMode.GRID) {
                        Icons.AutoMirrored.Filled.List
                    } else {
                        Icons.Default.GridView
                    },
                    contentDescription = strings.toggleView,
                )
            }

            if (hasItems) {
                IconButton(onClick = onSelectAll) {
                    Icon(Icons.Default.SelectAll, contentDescription = strings.selectAll)
                }
            }

            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = strings.refresh)
            }

            if (showUploadButton) {
                FilesUploadMenu(
                    expanded = showUploadMenu,
                    onDismissRequest = { onUploadMenuChange(false) },
                    onUploadAction = onUploadAction,
                    onUploadMenuChange = onUploadMenuChange,
                )
            }

            if (showEmptyTrashButton) {
                IconButton(onClick = onEmptyTrash) {
                    Icon(Icons.Default.DeleteForever, contentDescription = strings.emptyTrash)
                }
            }
        },
    )
}

@Composable
private fun BoxWithSortMenu(
    showSortMenu: Boolean,
    onDismissRequest: () -> Unit,
    sortField: SortField,
    sortOrder: SortOrder,
    onSortField: (SortField) -> Unit,
    onSortOrder: (SortOrder) -> Unit,
    onSortMenuChange: (Boolean) -> Unit,
) {
    Box {
        IconButton(onClick = { onSortMenuChange(true) }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
        }
        DropdownMenu(
            expanded = showSortMenu,
            onDismissRequest = onDismissRequest,
        ) {
            SortField.entries.forEach { field ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when (field) {
                                    SortField.NAME -> "Name"
                                    SortField.SIZE -> "Size"
                                    SortField.CREATED_AT -> "Created"
                                    SortField.UPDATED_AT -> "Modified"
                                    SortField.TYPE -> "Type"
                                },
                            )
                            if (sortField == field) {
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    if (sortOrder == SortOrder.ASC) {
                                        Icons.Default.ArrowUpward
                                    } else {
                                        Icons.Default.ArrowDownward
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    },
                    onClick = {
                        if (sortField == field) {
                            onSortOrder(
                                if (sortOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC,
                            )
                        } else {
                            onSortField(field)
                        }
                        onSortMenuChange(false)
                    },
                )
            }
        }
    }
}

@Composable
private fun FilesUploadMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onUploadAction: (UploadAction) -> Unit,
    onUploadMenuChange: (Boolean) -> Unit,
) {
    IconButton(onClick = { onUploadMenuChange(true) }) {
        Icon(Icons.Default.CloudUpload, contentDescription = "Upload")
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuItem(
            text = { Text("Upload files") },
            onClick = {
                onUploadMenuChange(false)
                onUploadAction(UploadAction.Files)
            },
            leadingIcon = {
                Icon(Icons.Default.UploadFile, contentDescription = null)
            },
        )
        DropdownMenuItem(
            text = { Text("Upload folder") },
            onClick = {
                onUploadMenuChange(false)
                onUploadAction(UploadAction.Folder)
            },
            leadingIcon = {
                Icon(Icons.Default.Folder, contentDescription = null)
            },
        )
        DropdownMenuItem(
            text = { Text("Upload large file(s)") },
            onClick = {
                onUploadMenuChange(false)
                onUploadAction(UploadAction.LargeFiles)
            },
            leadingIcon = {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
            },
        )
    }
}
