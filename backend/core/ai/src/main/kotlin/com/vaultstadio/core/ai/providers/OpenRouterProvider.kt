/**
 * OpenRouter AI Provider
 *
 * Provides access to multiple AI models through OpenRouter.
 * Supports vision models, chat, and various LLMs.
 *
 * @see https://openrouter.ai/docs
 */

package com.vaultstadio.core.ai.providers

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.ai.AIError
import com.vaultstadio.core.ai.AIMessage
import com.vaultstadio.core.ai.AIModel
import com.vaultstadio.core.ai.AIProvider
import com.vaultstadio.core.ai.AIProviderConfig
import com.vaultstadio.core.ai.AIProviderType
import com.vaultstadio.core.ai.AIRequest
import com.vaultstadio.core.ai.AIResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

private val logger = KotlinLogging.logger {}

/**
 * OpenRouter provider for accessing multiple AI models.
 *
 * Popular models available:
 * - anthropic/claude-3-opus
 * - anthropic/claude-3-sonnet
 * - openai/gpt-4-turbo
 * - google/gemini-pro-vision
 * - meta-llama/llama-3-70b
 * - mistralai/mixtral-8x22b
 */
class OpenRouterProvider(
    override val config: AIProviderConfig,
) : AIProvider {

    override val type = AIProviderType.OPENROUTER

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeout
            connectTimeoutMillis = 10000
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://openrouter.ai/api/v1"

        // Popular vision-capable models
        val VISION_MODELS = setOf(
            "openai/gpt-4-vision-preview",
            "openai/gpt-4o",
            "anthropic/claude-3-opus",
            "anthropic/claude-3-sonnet",
            "anthropic/claude-3-haiku",
            "google/gemini-pro-vision",
            "google/gemini-1.5-pro",
            "meta-llama/llama-3.2-90b-vision-instruct",
        )
    }

    override suspend fun isAvailable(): Boolean {
        if (config.apiKey.isNullOrBlank()) {
            logger.warn { "OpenRouter API key not configured" }
            return false
        }

        return try {
            val response = client.get("${config.baseUrl}/models") {
                header("Authorization", "Bearer ${config.apiKey}")
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            logger.warn { "OpenRouter not available: ${e.message}" }
            false
        }
    }

    override suspend fun listModels(): Either<AIError, List<AIModel>> {
        if (config.apiKey.isNullOrBlank()) {
            return AIError.AuthenticationError("OpenRouter API key not configured").left()
        }

        return try {
            val response = client.get("${config.baseUrl}/models") {
                header("Authorization", "Bearer ${config.apiKey}")
            }

            if (response.status == HttpStatusCode.Unauthorized) {
                return AIError.AuthenticationError("Invalid OpenRouter API key").left()
            }

            if (response.status != HttpStatusCode.OK) {
                return AIError.ProviderError("Failed to list models: ${response.status}").left()
            }

            val modelsResponse = response.body<OpenRouterModelsResponse>()

            val models = modelsResponse.data.map { m ->
                AIModel(
                    id = m.id,
                    name = m.name ?: m.id,
                    provider = "openrouter",
                    supportsVision = VISION_MODELS.contains(m.id) ||
                        m.id.contains("vision", ignoreCase = true),
                    contextLength = m.contextLength ?: 4096,
                    description = m.description,
                )
            }
            models.right()
        } catch (e: Exception) {
            logger.error(e) { "Failed to list OpenRouter models" }
            AIError.ConnectionError("Failed to connect to OpenRouter: ${e.message}").left()
        }
    }

    override suspend fun chat(request: AIRequest): Either<AIError, AIResponse> {
        if (config.apiKey.isNullOrBlank()) {
            return AIError.AuthenticationError("OpenRouter API key not configured").left()
        }

        return try {
            val model = request.model ?: config.model

            // Convert messages to OpenRouter format
            val messages = request.messages.map { msg ->
                if (msg.images != null && msg.images.isNotEmpty()) {
                    // Multimodal message with images
                    OpenRouterMessage(
                        role = msg.role,
                        content = buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("type", "text")
                                    put("text", msg.content)
                                },
                            )
                            msg.images.forEach { image ->
                                add(
                                    buildJsonObject {
                                        put("type", "image_url")
                                        putJsonObject("image_url") {
                                            put("url", "data:image/jpeg;base64,$image")
                                        }
                                    },
                                )
                            }
                        },
                    )
                } else {
                    // Text-only message
                    OpenRouterMessage(
                        role = msg.role,
                        content = JsonPrimitive(msg.content),
                    )
                }
            }

            val openRouterRequest = OpenRouterChatRequest(
                model = model,
                messages = messages,
                maxTokens = request.maxTokens ?: config.maxTokens,
                temperature = request.temperature ?: config.temperature,
                stream = request.stream,
            )

            val chatResponse = client.post("${config.baseUrl}/chat/completions") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer ${config.apiKey}")
                header("HTTP-Referer", "https://vaultstadio.io")
                header("X-Title", "VaultStadio")
                setBody(openRouterRequest)
            }

            when (chatResponse.status) {
                HttpStatusCode.Unauthorized ->
                    return AIError.AuthenticationError("Invalid API key").left()
                HttpStatusCode.TooManyRequests ->
                    return AIError.RateLimitError("Rate limit exceeded").left()
                HttpStatusCode.NotFound ->
                    return AIError.ModelNotFoundError("Model not found: $model").left()
                else -> {}
            }

            if (!chatResponse.status.isSuccess()) {
                val errorText = chatResponse.bodyAsText()
                return AIError.ProviderError("OpenRouter error: $errorText").left()
            }

            val openRouterResponse = chatResponse.body<OpenRouterChatResponse>()
            val choice = openRouterResponse.choices.firstOrNull()

            AIResponse(
                content = choice?.message?.content ?: "",
                model = openRouterResponse.model ?: model,
                promptTokens = openRouterResponse.usage?.promptTokens ?: 0,
                completionTokens = openRouterResponse.usage?.completionTokens ?: 0,
                totalTokens = openRouterResponse.usage?.totalTokens ?: 0,
                finishReason = choice?.finishReason,
            ).right()
        } catch (e: Exception) {
            logger.error(e) { "OpenRouter chat failed" }
            AIError.ProviderError("OpenRouter chat failed: ${e.message}").left()
        }
    }

    override suspend fun vision(
        prompt: String,
        imageBase64: String,
        mimeType: String,
    ): Either<AIError, AIResponse> {
        // Use a vision-capable model
        val visionModel = if (VISION_MODELS.contains(config.model)) {
            config.model
        } else {
            "anthropic/claude-3-haiku" // Default to a fast, capable vision model
        }

        return chat(
            AIRequest(
                messages = listOf(
                    AIMessage(
                        role = "user",
                        content = prompt,
                        images = listOf(imageBase64),
                    ),
                ),
                model = visionModel,
            ),
        )
    }

    fun close() {
        client.close()
    }
}

// OpenRouter API Models
@Serializable
data class OpenRouterModelsResponse(
    val data: List<OpenRouterModel>,
)

@Serializable
data class OpenRouterModel(
    val id: String,
    val name: String? = null,
    val description: String? = null,
    @SerialName("context_length")
    val contextLength: Int? = null,
    val pricing: OpenRouterPricing? = null,
)

@Serializable
data class OpenRouterPricing(
    val prompt: String? = null,
    val completion: String? = null,
)

@Serializable
data class OpenRouterChatRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    val temperature: Double? = null,
    val stream: Boolean = false,
)

@Serializable
data class OpenRouterMessage(
    val role: String,
    val content: JsonElement, // Can be string or array of content parts
)

@Serializable
data class OpenRouterChatResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<OpenRouterChoice>,
    val usage: OpenRouterUsage? = null,
)

@Serializable
data class OpenRouterChoice(
    val message: OpenRouterMessageContent? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null,
)

@Serializable
data class OpenRouterMessageContent(
    val role: String? = null,
    val content: String? = null,
)

@Serializable
data class OpenRouterUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0,
)
