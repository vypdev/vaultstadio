/**
 * AI Summarize Request DTO
 */

package com.vaultstadio.app.data.ai.dto

import kotlinx.serialization.Serializable

@Serializable
data class AISummarizeRequestDTO(val text: String, val maxLength: Int = 200)
