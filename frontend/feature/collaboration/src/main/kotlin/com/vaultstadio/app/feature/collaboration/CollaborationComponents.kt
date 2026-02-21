package com.vaultstadio.app.feature.collaboration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.collaboration.model.CommentReply
import com.vaultstadio.app.domain.collaboration.model.CollaborationParticipant
import com.vaultstadio.app.domain.collaboration.model.DocumentComment

@Composable
fun ParticipantsAvatars(
    participants: List<CollaborationParticipant>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(end = 8.dp)) {
        participants.take(3).forEachIndexed { index, participant ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(parseColor(participant.color))
                    .padding(start = if (index > 0) 0.dp else 0.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    participant.userName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (participants.size > 3) {
            IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
                Text(
                    "+${participants.size - 3}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
fun ParticipantItem(
    participant: CollaborationParticipant,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(parseColor(participant.color)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                participant.userName.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                participant.userName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            if (participant.isEditing) {
                Text(
                    "Editing...",
                    style = MaterialTheme.typography.bodySmall,
                    color = parseColor(participant.color),
                )
            }
        }

        if (participant.isEditing) {
            Icon(
                Icons.Default.Edit,
                null,
                modifier = Modifier.size(16.dp),
                tint = parseColor(participant.color),
            )
        }
    }
}

@Composable
fun CommentItem(
    comment: DocumentComment,
    onResolve: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (comment.isResolved) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            CommentHeader(
                userName = comment.userName ?: "Unknown",
                isResolved = comment.isResolved,
                onResolve = onResolve,
                onDeleteClick = { showDeleteConfirm = true },
            )

            Spacer(Modifier.height(4.dp))

            if (showDeleteConfirm) {
                DeleteConfirmation(
                    onCancel = { showDeleteConfirm = false },
                    onConfirm = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                )
            }

            Text(comment.content, style = MaterialTheme.typography.bodySmall)

            comment.anchor.quotedText?.let { quotedText ->
                Spacer(Modifier.height(4.dp))
                QuotedTextBlock(text = quotedText)
            }

            Text(
                formatCollaborationRelativeTime(comment.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (comment.replies.isNotEmpty()) {
                CommentReplies(replies = comment.replies)
            }
        }
    }
}

@Composable
private fun CommentHeader(
    userName: String,
    isResolved: Boolean,
    onResolve: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                userName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
        }

        Row {
            if (!isResolved) {
                IconButton(onClick = onResolve, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.CheckCircle,
                        "Resolve",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Close,
                    "Delete",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmation(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onCancel) {
            Text("Cancel", style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onConfirm) {
            Text(
                "Delete",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun QuotedTextBlock(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            "\"$text\"",
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CommentReplies(replies: List<CommentReply>) {
    Spacer(Modifier.height(8.dp))
    HorizontalDivider()
    replies.forEach { reply ->
        Spacer(Modifier.height(8.dp))
        Row {
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    reply.userName ?: "Unknown",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    reply.content,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

fun parseColor(hex: String): Color {
    return try {
        val colorString = hex.removePrefix("#")
        val colorLong = colorString.toLong(16)
        when (colorString.length) {
            6 -> Color(0xFF000000 or colorLong)
            8 -> Color(colorLong)
            else -> Color(0xFF6200EE)
        }
    } catch (_: Exception) {
        Color(0xFF6200EE)
    }
}
