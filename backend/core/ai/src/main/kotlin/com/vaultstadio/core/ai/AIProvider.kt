/**
 * VaultStadio AI Provider Abstraction
 *
 * Provides a unified interface for interacting with different AI providers:
 * - Local models (Ollama, LM Studio)
 * - OpenRouter (access to multiple cloud models)
 * - OpenAI
 * - Custom endpoints
 */

package com.vaultstadio.core.ai

import arrow.core.Either
import arrow.core.left
import kotlinx.serialization.Serializable

/**
 * Supported AI provider types.
 */
enum class AIProviderType {
    OLLAMA,
    LM_STUDIO,
    OPENROUTER,
    OPENAI,
    CUSTOM,
}

/**
 * Configuration for an AI provider.
 */
@Serializable
data class AIProviderConfig(
    val type: AIProviderType,
    val baseUrl: String,
    val apiKey: String? = null,
    val model: String,
    val timeout: Long = 120000,
    val maxTokens: Int = 1024,
    val temperature: Double = 0.7,
    val enabled: Boolean = true,
)

/**
 * A message in a conversation.
 */
@Serializable
data class AIMessage(
    val role: String,
    val content: String,
    val images: List<String>? = null, // Base64 encoded images
)

/**
 * Request to an AI provider.
 */
@Serializable
data class AIRequest(
    val messages: List<AIMessage>,
    val model: String? = null,
    val maxTokens: Int? = null,
    val temperature: Double? = null,
    val stream: Boolean = false,
)

/**
 * Response from an AI provider.
 */
@Serializable
data class AIResponse(
    val content: String,
    val model: String,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val finishReason: String? = null,
)

/**
 * Error from an AI provider.
 */
sealed class AIError(val errorMessage: String) : Exception(errorMessage) {
    data class ConnectionError(val msg: String) : AIError(msg)
    data class AuthenticationError(val msg: String) : AIError(msg)
    data class RateLimitError(val msg: String) : AIError(msg)
    data class ModelNotFoundError(val msg: String) : AIError(msg)
    data class InvalidRequestError(val msg: String) : AIError(msg)
    data class ProviderError(val msg: String) : AIError(msg)
}

/**
 * Model information from a provider.
 */
@Serializable
data class AIModel(
    val id: String,
    val name: String,
    val provider: String,
    val supportsVision: Boolean = false,
    val contextLength: Int = 4096,
    val description: String? = null,
)

/**
 * AI Provider interface.
 */
interface AIProvider {
    val type: AIProviderType
    val config: AIProviderConfig

    /**
     * Check if the provider is available and configured correctly.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Get available models from this provider.
     */
    suspend fun listModels(): Either<AIError, List<AIModel>>

    /**
     * Send a chat completion request.
     */
    suspend fun chat(request: AIRequest): Either<AIError, AIResponse>

    /**
     * Send a vision request (image analysis).
     */
    suspend fun vision(
        prompt: String,
        imageBase64: String,
        mimeType: String = "image/jpeg",
    ): Either<AIError, AIResponse>

    /**
     * Generate embeddings for text.
     */
    suspend fun embeddings(text: String): Either<AIError, List<Double>> =
        AIError.ProviderError("Embeddings not supported by this provider").left()
}
