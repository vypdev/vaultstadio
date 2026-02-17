/**
 * AI Domain Models
 */

package com.vaultstadio.app.domain.model

enum class AIProviderType {
    OLLAMA,
    LM_STUDIO,
    OPENROUTER,
}

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

data class AIModel(
    val id: String,
    val name: String,
    val provider: AIProviderType,
    val supportsVision: Boolean = false,
    val contextLength: Int? = null,
    val description: String? = null,
)

enum class ChatRole { SYSTEM, USER, ASSISTANT }

data class AIChatMessage(
    val role: ChatRole,
    val content: String,
    val images: List<String>? = null,
)

data class AIChatResponse(
    val content: String,
    val model: String,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null,
)

/** Request body for chat completion. */
data class AIChatRequest(
    val messages: List<AIChatMessage>,
    val model: String? = null,
    val maxTokens: Int? = null,
    val temperature: Double? = null,
)

/** Request for vision/describe image. */
data class AIVisionRequest(
    val prompt: String,
    val imageBase64: String,
    val mimeType: String = "image/jpeg",
)

/** Request for AI describe (item). */
data class AIDescribeRequest(
    val itemId: String,
    val prompt: String? = null,
)

/** Request for summarization. */
data class AISummarizeRequest(
    val itemId: String,
    val maxLength: Int? = null,
)

/** Request for tagging. */
data class AITagRequest(
    val itemIds: List<String>,
    val tags: List<String>,
)

/** Request for classification. */
data class AIClassifyRequest(
    val itemId: String,
    val categories: List<String>? = null,
)
