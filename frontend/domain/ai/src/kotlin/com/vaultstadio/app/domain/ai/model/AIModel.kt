/**
 * AI Model
 */

package com.vaultstadio.app.domain.ai.model

data class AIModel(
    val id: String,
    val name: String,
    val provider: AIProviderType,
    val supportsVision: Boolean = false,
    val contextLength: Int? = null,
    val description: String? = null,
)
