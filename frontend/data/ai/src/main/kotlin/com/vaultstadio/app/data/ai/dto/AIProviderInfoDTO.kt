/**
 * AI Provider Info DTO
 */

package com.vaultstadio.app.data.ai.dto

import kotlinx.serialization.Serializable

@Serializable
data class AIProviderInfoDTO(
    val type: String,
    val baseUrl: String,
    val model: String? = null,
    val hasApiKey: Boolean = false,
    val timeout: Long = 30000,
    val maxTokens: Int = 2048,
    val temperature: Double = 0.7,
    val enabled: Boolean = true,
    val isActive: Boolean = false,
)
