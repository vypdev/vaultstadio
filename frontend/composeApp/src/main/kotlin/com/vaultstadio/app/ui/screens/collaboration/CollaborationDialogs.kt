/**
 * VaultStadio Collaboration Dialogs
 *
 * Dialog components for the Collaboration screen.
 */

package com.vaultstadio.app.ui.screens.collaboration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.collaboration.model.CollaborationParticipant
import com.vaultstadio.app.domain.collaboration.model.CommentAnchor
import com.vaultstadio.app.domain.collaboration.model.DocumentComment
import com.vaultstadio.app.core.resources.StringResources
import com.vaultstadio.app.core.resources.Strings
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import kotlin.time.Clock
import org.jetbrains.compose.ui.tooling.preview.Preview

private val SampleParticipant = CollaborationParticipant(
    id = "participant-1",
    userId = "user-1",
    userName = "John Doe",
    color = "#6200EE",
    isEditing = true,
)

private val SampleComment = DocumentComment(
    id = "comment-1",
    itemId = "item-1",
    userId = "user-1",
    userName = "Jane Smith",
    content = "This looks good, but we might want to refactor this section.",
    createdAt = Clock.System.now(),
    updatedAt = Clock.System.now(),
    isResolved = false,
    anchor = CommentAnchor(
        startLine = 10,
        startColumn = 0,
        endLine = 10,
        endColumn = 20,
        quotedText = "fun processData()",
    ),
    replies = emptyList(),
)

/**
 * Dialog showing session participants.
 */
@Composable
fun ParticipantsDialog(
    participants: List<CollaborationParticipant>,
    isConnected: Boolean,
    onDismiss: () -> Unit,
    strings: StringResources,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(strings.collaborationParticipants)
                ConnectionStatusIndicator(isConnected = isConnected)
            }
        },
        text = {
            LazyColumn {
                items(participants) { participant ->
                    ParticipantItem(participant = participant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionClose)
            }
        },
    )
}

@Composable
private fun ConnectionStatusIndicator(isConnected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isConnected) Color(0xFF4CAF50) else Color(0xFFE53935)),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            if (isConnected) "Connected" else "Disconnected",
            style = MaterialTheme.typography.bodySmall,
            color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFE53935),
        )
    }
}

/**
 * Dialog showing document comments.
 */
@Composable
fun CommentsDialog(
    comments: List<DocumentComment>,
    onResolve: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAddComment: () -> Unit,
    onDismiss: () -> Unit,
    strings: StringResources,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(strings.collaborationComments)
                IconButton(onClick = onAddComment) {
                    Icon(Icons.AutoMirrored.Filled.Comment, strings.collaborationAddComment)
                }
            }
        },
        text = {
            if (comments.isEmpty()) {
                EmptyCommentsState(strings = strings)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(comments) { comment ->
                        CommentItem(
                            comment = comment,
                            onResolve = { onResolve(comment.id) },
                            onDelete = { onDelete(comment.id) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionClose)
            }
        },
    )
}

@Composable
private fun EmptyCommentsState(strings: StringResources) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Comment,
            null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            strings.collaborationNoCommentsYet,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Dialog for adding a new comment.
 */
@Composable
fun AddCommentDialog(
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
    strings: StringResources,
) {
    var commentText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.collaborationAddComment) },
        text = {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                label = { Text(strings.collaborationComment) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (commentText.isNotBlank()) {
                        onAdd(commentText)
                    }
                },
            ) {
                Text(strings.commonAdd)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel)
            }
        },
    )
}

/**
 * Generic error dialog.
 */
@Composable
fun CollaborationErrorDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}

// region Previews

@Preview
@Composable
internal fun ParticipantsDialogPreview() {
    VaultStadioPreview {
        ParticipantsDialog(
            participants = listOf(
                SampleParticipant,
                SampleParticipant.copy(userId = "user-2", userName = "Alice", color = "#03DAC5"),
            ),
            isConnected = true,
            onDismiss = {},
            strings = Strings.resources,
        )
    }
}

@Preview
@Composable
internal fun CommentsDialogPreview() {
    VaultStadioPreview {
        CommentsDialog(
            comments = listOf(SampleComment),
            onResolve = {},
            onDelete = {},
            onAddComment = {},
            onDismiss = {},
            strings = Strings.resources,
        )
    }
}

@Preview
@Composable
internal fun AddCommentDialogPreview() {
    VaultStadioPreview {
        AddCommentDialog(
            onAdd = {},
            onDismiss = {},
            strings = Strings.resources,
        )
    }
}

@Preview
@Composable
internal fun CollaborationErrorDialogPreview() {
    VaultStadioPreview {
        CollaborationErrorDialog(
            message = "Failed to connect to collaboration session",
            onDismiss = {},
        )
    }
}

// endregion
