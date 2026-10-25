/**
 * AI Model DTO
 */

package com.vaultstadio.app.data.ai.dto

import kotlinx.serialization.Serializable

@Serializable
data class AIModelDTO(
    val id: String,
    val name: String,
    val provider: String,
    val supportsVision: Boolean = false,
    val contextLength: Int? = null,
    val description: String? = null,
)
