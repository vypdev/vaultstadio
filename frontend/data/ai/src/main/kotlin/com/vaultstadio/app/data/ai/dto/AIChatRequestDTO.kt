/**
 * AI Chat Request DTO
 */

package com.vaultstadio.app.data.ai.dto

import kotlinx.serialization.Serializable

@Serializable
data class AIChatRequestDTO(
    val messages: List<AIChatMessageDTO>,
    val model: String? = null,
    val maxTokens: Int? = null,
    val temperature: Double? = null,
)
