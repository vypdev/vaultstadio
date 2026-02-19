/**
 * AI Chat Message
 */

package com.vaultstadio.app.domain.ai.model

data class AIChatMessage(
    val role: ChatRole,
    val content: String,
    val images: List<String>? = null,
)
