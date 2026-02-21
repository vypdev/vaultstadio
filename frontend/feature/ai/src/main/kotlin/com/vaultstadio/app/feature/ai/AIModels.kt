package com.vaultstadio.app.feature.ai

import com.vaultstadio.app.domain.ai.model.ChatRole

enum class AIMode {
    CHAT,
    DESCRIBE,
    TAG,
    CLASSIFY,
    SUMMARIZE,
}

data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val isLoading: Boolean = false,
)
