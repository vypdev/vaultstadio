/**
 * AI Chat Response DTO
 */

package com.vaultstadio.app.data.ai.dto

import kotlinx.serialization.Serializable

@Serializable
data class AIChatResponseDTO(
    val content: String,
    val model: String,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null,
)
