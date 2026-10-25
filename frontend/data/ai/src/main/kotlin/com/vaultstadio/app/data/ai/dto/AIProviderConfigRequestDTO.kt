/**
 * AI Provider Config Request DTO
 */

package com.vaultstadio.app.data.ai.dto

import kotlinx.serialization.Serializable

@Serializable
data class AIProviderConfigRequestDTO(
    val type: String,
    val baseUrl: String,
    val apiKey: String? = null,
    val model: String? = null,
    val timeout: Long = 30000,
    val maxTokens: Int = 2048,
    val temperature: Double = 0.7,
    val enabled: Boolean = true,
)
