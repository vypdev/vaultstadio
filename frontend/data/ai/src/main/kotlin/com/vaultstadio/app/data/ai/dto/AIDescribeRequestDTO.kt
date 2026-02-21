/**
 * AI Describe Request DTO
 */

package com.vaultstadio.app.data.ai.dto

import kotlinx.serialization.Serializable

@Serializable
data class AIDescribeRequestDTO(val imageBase64: String, val mimeType: String = "image/jpeg")
