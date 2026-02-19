/**
 * AI Provider Info
 */

package com.vaultstadio.app.domain.ai.model

data class AIProviderInfo(
    val type: AIProviderType,
    val baseUrl: String,
    val model: String? = null,
    val hasApiKey: Boolean = false,
    val timeout: Long = 30000,
    val maxTokens: Int = 2048,
    val temperature: Double = 0.7,
    val enabled: Boolean = true,
    val isActive: Boolean = false,
)
