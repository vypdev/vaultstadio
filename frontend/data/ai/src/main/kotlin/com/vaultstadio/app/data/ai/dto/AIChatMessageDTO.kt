/**
 * AI Chat Message DTO
 */

package com.vaultstadio.app.data.ai.dto

import kotlinx.serialization.Serializable

@Serializable
data class AIChatMessageDTO(
    val role: String,
    val content: String,
    val images: List<String>? = null,
)
