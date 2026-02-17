/**
 * VaultStadio Shared With Me Screen
 *
 * Screen displaying files and folders that have been shared with the current user.
 */

package com.vaultstadio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.StorageItem
import com.vaultstadio.app.i18n.strings
import com.vaultstadio.app.ui.screens.shared.DownloadReadyDialog
import com.vaultstadio.app.ui.screens.shared.EmptySharedWithMeState
import com.vaultstadio.app.ui.screens.shared.ItemDetailsDialog
import com.vaultstadio.app.ui.screens.shared.SharedErrorDialog
import com.vaultstadio.app.ui.screens.shared.SharedWithMeItemCard
import kotlinx.datetime.Instant

/**
 * Model for shared item with sharing info.
 */
data class SharedWithMeItem(
    val item: StorageItem,
    val sharedBy: String,
    val sharedByEmail: String,
    val sharedAt: Instant,
    val permissions: List<String>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedWithMeScreen(
    sharedItems: List<SharedWithMeItem>,
    selectedItem: StorageItem?,
    downloadUrl: String?,
    isLoading: Boolean,
    error: String?,
    onLoadSharedItems: () -> Unit,
    onItemClick: (StorageItem) -> Unit,
    onDownload: (StorageItem) -> Unit,
    onRemoveShare: (String) -> Unit,
    onClearSelectedItem: () -> Unit,
    onClearDownloadUrl: () -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = strings()
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        onLoadSharedItems()
    }

    val files = sharedItems.filter { !it.item.isFolder }
    val folders = sharedItems.filter { it.item.isFolder }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.navShared) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, strings.commonBack)
                    }
                },
                actions = {
                    IconButton(onClick = onLoadSharedItems) {
                        Icon(Icons.Default.Refresh, strings.actionRefresh)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = modifier.fillMaxSize().padding(padding),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("All (${sharedItems.size})") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Folders (${folders.size})")
                        }
                    },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Files (${files.size})")
                        }
                    },
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    sharedItems.isEmpty() -> {
                        EmptySharedWithMeState(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    else -> {
                        val itemsToShow = when (selectedTab) {
                            1 -> folders
                            2 -> files
                            else -> sharedItems
                        }
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(itemsToShow) { sharedItem ->
                                SharedWithMeItemCard(
                                    sharedItem = sharedItem,
                                    onItemClick = { onItemClick(sharedItem.item) },
                                    onDownload = { onDownload(sharedItem.item) },
                                    onRemoveShare = { onRemoveShare(sharedItem.item.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedItem?.let { item ->
        ItemDetailsDialog(item = item, onDismiss = onClearSelectedItem)
    }
    downloadUrl?.let { url ->
        DownloadReadyDialog(url = url, onDismiss = onClearDownloadUrl)
    }
    error?.let { message ->
        SharedErrorDialog(message = message, onDismiss = onClearError)
    }
}
