/**
 * VaultStadio Shared Screen
 *
 * Screen for managing shared files and links.
 */

package com.vaultstadio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.ShareLink
import com.vaultstadio.app.ui.components.files.EmptyState
import com.vaultstadio.app.ui.screens.shared.LinkCopiedDialog
import com.vaultstadio.app.ui.screens.shared.ShareLinkCard
import com.vaultstadio.app.ui.screens.shared.SharedErrorDialog

/**
 * Shared items screen showing all shared links.
 */
@Composable
fun SharedScreen(
    shares: List<ShareLink>,
    isLoading: Boolean,
    error: String?,
    clipboardLink: String?,
    onCopyLink: (ShareLink) -> Unit,
    onDeleteShare: (ShareLink) -> Unit,
    onRefresh: () -> Unit,
    onClearClipboardLink: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Shared Links",
                style = MaterialTheme.typography.headlineSmall,
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
        }

        HorizontalDivider()

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            shares.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = Icons.Filled.Share,
                        title = "No shared links",
                        description = "Files you share will appear here. Share a file by " +
                            "right-clicking and selecting 'Share'.",
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(shares) { share ->
                        ShareLinkCard(
                            share = share,
                            onCopyLink = { onCopyLink(share) },
                            onDelete = { onDeleteShare(share) },
                        )
                    }
                }
            }
        }
    }

    clipboardLink?.let { link ->
        LinkCopiedDialog(link = link, onDismiss = onClearClipboardLink)
    }
    error?.let { message ->
        SharedErrorDialog(message = message, onDismiss = onClearError)
    }
}
