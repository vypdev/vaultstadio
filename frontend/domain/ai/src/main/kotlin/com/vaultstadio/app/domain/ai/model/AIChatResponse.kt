/**
 * AI Chat Response
 */

package com.vaultstadio.app.domain.ai.model

data class AIChatResponse(
    val content: String,
    val model: String,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null,
)
