/**
 * AI Data Transfer Objects
 */

package com.vaultstadio.app.data.dto.ai

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

@Serializable
data class SetActiveProviderRequestDTO(val type: String)

@Serializable
data class AIModelDTO(
    val id: String,
    val name: String,
    val provider: String,
    val supportsVision: Boolean = false,
    val contextLength: Int? = null,
    val description: String? = null,
)

@Serializable
data class AIChatMessageDTO(
    val role: String,
    val content: String,
    val images: List<String>? = null,
)

@Serializable
data class AIChatRequestDTO(
    val messages: List<AIChatMessageDTO>,
    val model: String? = null,
    val maxTokens: Int? = null,
    val temperature: Double? = null,
)

@Serializable
data class AIChatResponseDTO(
    val content: String,
    val model: String,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null,
)

@Serializable
data class AIDescribeRequestDTO(val imageBase64: String, val mimeType: String = "image/jpeg")

@Serializable
data class AITagRequestDTO(val imageBase64: String, val mimeType: String = "image/jpeg")

@Serializable
data class AIClassifyRequestDTO(val content: String, val categories: List<String>)

@Serializable
data class AISummarizeRequestDTO(val text: String, val maxLength: Int = 200)
