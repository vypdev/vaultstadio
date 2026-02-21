/**
 * AI Classify Request DTO
 */

package com.vaultstadio.app.data.ai.dto

import kotlinx.serialization.Serializable

@Serializable
data class AIClassifyRequestDTO(val content: String, val categories: List<String>)
