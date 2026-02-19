/**
 * VaultStadio AI Models
 *
 * Data models used by the AI screen.
 */

package com.vaultstadio.app.ui.screens.ai

import com.vaultstadio.app.domain.ai.model.ChatRole

/**
 * AI mode selection.
 */
enum class AIMode {
    CHAT,
    DESCRIBE,
    TAG,
    CLASSIFY,
    SUMMARIZE,
}

/**
 * Chat message for UI.
 */
data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val isLoading: Boolean = false,
)
