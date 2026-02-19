/**
 * Request body for chat completion.
 */

package com.vaultstadio.app.domain.ai.model

data class AIChatRequest(
    val messages: List<AIChatMessage>,
    val model: String? = null,
    val maxTokens: Int? = null,
    val temperature: Double? = null,
)
