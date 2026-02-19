/**
 * VaultStadio AI Chat Components
 *
 * Chat-related UI components for the AI screen.
 */

package com.vaultstadio.app.ui.screens.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.ai.model.ChatRole
import com.vaultstadio.app.i18n.Strings
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Chat bubble displaying a message.
 */
@Composable
fun ChatBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == ChatRole.USER

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            if (message.isLoading) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        Strings.resources.aiThinking,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

// region Previews

@Preview
@Composable
internal fun ChatBubbleUserPreview() {
    VaultStadioPreview {
        ChatBubble(
            message = ChatMessage(
                role = ChatRole.USER,
                content = "Hello! Can you help me organize my files?",
            ),
        )
    }
}

@Preview
@Composable
internal fun ChatBubbleAssistantPreview() {
    VaultStadioPreview {
        ChatBubble(
            message = ChatMessage(
                role = ChatRole.ASSISTANT,
                content = "Of course! I can help you organize your files. What would you like to do?",
            ),
        )
    }
}

@Preview
@Composable
internal fun ChatBubbleLoadingPreview() {
    VaultStadioPreview {
        ChatBubble(
            message = ChatMessage(
                role = ChatRole.ASSISTANT,
                content = "",
                isLoading = true,
            ),
        )
    }
}

// endregion
