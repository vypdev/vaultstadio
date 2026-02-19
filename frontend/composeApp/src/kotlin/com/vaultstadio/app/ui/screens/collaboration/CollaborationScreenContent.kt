/**
 * VaultStadio Collaboration Screen Content
 *
 * Top bar, empty/loading states, and document editor for the Collaboration screen.
 * Extracted to keep CollaborationScreen under the line limit.
 */

package com.vaultstadio.app.ui.screens.collaboration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultstadio.app.domain.collaboration.model.CollaborationSession
import com.vaultstadio.app.domain.collaboration.model.DocumentComment
import com.vaultstadio.app.domain.collaboration.model.DocumentState
import com.vaultstadio.app.i18n.StringResources
import com.vaultstadio.app.utils.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollaborationTopBar(
    itemName: String,
    session: CollaborationSession?,
    comments: List<DocumentComment>,
    isSaving: Boolean,
    showMoreMenu: Boolean,
    onBack: () -> Unit,
    onShowParticipants: () -> Unit,
    onShowComments: () -> Unit,
    onSave: () -> Unit,
    onShowMoreMenu: () -> Unit,
    onHideMoreMenu: () -> Unit,
    onAddComment: () -> Unit,
    strings: StringResources,
) {
    TopAppBar(
        title = {
            Column {
                Text(itemName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (session != null) {
                    Text(
                        "${session.participants.size} participant${if (session.participants.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            if (session != null && session.participants.isNotEmpty()) {
                ParticipantsAvatars(
                    participants = session.participants,
                    onClick = onShowParticipants,
                )
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = onShowComments) {
                Box {
                    Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Comments")
                    if (comments.any { !it.isResolved }) {
                        Surface(
                            modifier = Modifier.size(8.dp).align(Alignment.TopEnd),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error,
                        ) {}
                    }
                }
            }
            IconButton(onClick = onSave, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                }
            }
            Box {
                IconButton(onClick = onShowMoreMenu) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = showMoreMenu, onDismissRequest = onHideMoreMenu) {
                    DropdownMenuItem(
                        text = { Text(strings.collaborationAddComment) },
                        onClick = {
                            onHideMoreMenu()
                            onAddComment()
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Comment, null) },
                    )
                }
            }
        },
    )
}

@Composable
fun CollaborationLoadingState(strings: StringResources) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(strings.collaborationJoiningSession)
        }
    }
}

@Composable
fun SelectFileToStartState(strings: StringResources) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                strings.collaborationSelectFileToStart,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun FailedToJoinState(
    itemId: String,
    onRetry: (String) -> Unit,
    strings: StringResources,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(16.dp))
            Text(strings.collaborationCouldNotJoin, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onRetry(itemId) }) {
                Text(strings.commonRetry)
            }
        }
    }
}

@Composable
fun DocumentEditor(
    documentState: DocumentState?,
    editedContent: String,
    onContentChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Version ${documentState?.version ?: 0}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                documentState?.lastModified?.let { lastModified ->
                    Text(
                        "Last saved: ${formatRelativeTime(lastModified)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        BasicTextField(
            value = editedContent,
            onValueChange = onContentChange,
            modifier = Modifier.fillMaxSize().padding(16.dp),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        )
    }
}
