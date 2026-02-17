/**
 * LM Studio AI Provider
 *
 * Supports local AI inference using LM Studio.
 * LM Studio exposes an OpenAI-compatible API.
 *
 * @see https://lmstudio.ai/
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
 * LM Studio provider for local AI inference.
 *
 * Uses OpenAI-compatible API format.
 * Default endpoint: http://localhost:1234/v1
 */
class LMStudioProvider(
    override val config: AIProviderConfig,
) : AIProvider {

    override val type = AIProviderType.LM_STUDIO

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
        const val DEFAULT_BASE_URL = "http://localhost:1234/v1"
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            val response = client.get("${config.baseUrl}/models")
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            logger.warn { "LM Studio not available: ${e.message}" }
            false
        }
    }

    override suspend fun listModels(): Either<AIError, List<AIModel>> {
        return try {
            val response = client.get("${config.baseUrl}/models")

            if (response.status != HttpStatusCode.OK) {
                return AIError.ProviderError("Failed to list models: ${response.status}").left()
            }

            val modelsResponse = response.body<LMStudioModelsResponse>()

            modelsResponse.data.map { model ->
                AIModel(
                    id = model.id,
                    name = model.id,
                    provider = "lm-studio",
                    supportsVision = model.id.contains("vision", ignoreCase = true) ||
                        model.id.contains("llava", ignoreCase = true),
                    contextLength = 4096,
                    description = "LM Studio local model",
                )
            }.right()
        } catch (e: Exception) {
            logger.error(e) { "Failed to list LM Studio models" }
            AIError.ConnectionError("Failed to connect to LM Studio: ${e.message}").left()
        }
    }

    override suspend fun chat(request: AIRequest): Either<AIError, AIResponse> {
        return try {
            val model = request.model ?: config.model

            // Convert messages to OpenAI format
            val messages = request.messages.map { msg ->
                if (msg.images != null && msg.images.isNotEmpty()) {
                    // Multimodal message with images
                    LMStudioMessage(
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
                    LMStudioMessage(
                        role = msg.role,
                        content = JsonPrimitive(msg.content),
                    )
                }
            }

            val lmStudioRequest = LMStudioChatRequest(
                model = model,
                messages = messages,
                maxTokens = request.maxTokens ?: config.maxTokens,
                temperature = request.temperature ?: config.temperature,
                stream = request.stream,
            )

            val response = client.post("${config.baseUrl}/chat/completions") {
                contentType(ContentType.Application.Json)
                setBody(lmStudioRequest)
            }

            if (!response.status.isSuccess()) {
                val error = response.bodyAsText()
                return AIError.ProviderError("LM Studio error: $error").left()
            }

            val lmStudioResponse = response.body<LMStudioChatResponse>()
            val choice = lmStudioResponse.choices.firstOrNull()

            AIResponse(
                content = choice?.message?.content ?: "",
                model = lmStudioResponse.model ?: model,
                promptTokens = lmStudioResponse.usage?.promptTokens ?: 0,
                completionTokens = lmStudioResponse.usage?.completionTokens ?: 0,
                totalTokens = lmStudioResponse.usage?.totalTokens ?: 0,
                finishReason = choice?.finishReason,
            ).right()
        } catch (e: Exception) {
            logger.error(e) { "LM Studio chat failed" }
            AIError.ProviderError("LM Studio chat failed: ${e.message}").left()
        }
    }

    override suspend fun vision(
        prompt: String,
        imageBase64: String,
        mimeType: String,
    ): Either<AIError, AIResponse> {
        return chat(
            AIRequest(
                messages = listOf(
                    AIMessage(
                        role = "user",
                        content = prompt,
                        images = listOf(imageBase64),
                    ),
                ),
                model = config.model,
            ),
        )
    }

    fun close() {
        client.close()
    }
}

// LM Studio API Models (OpenAI-compatible)
@Serializable
data class LMStudioModelsResponse(
    val data: List<LMStudioModel>,
)

@Serializable
data class LMStudioModel(
    val id: String,
    val `object`: String? = null,
)

@Serializable
data class LMStudioChatRequest(
    val model: String,
    val messages: List<LMStudioMessage>,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    val temperature: Double? = null,
    val stream: Boolean = false,
)

@Serializable
data class LMStudioMessage(
    val role: String,
    val content: JsonElement,
)

@Serializable
data class LMStudioChatResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<LMStudioChoice>,
    val usage: LMStudioUsage? = null,
)

@Serializable
data class LMStudioChoice(
    val message: LMStudioMessageContent? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null,
)

@Serializable
data class LMStudioMessageContent(
    val role: String? = null,
    val content: String? = null,
)

@Serializable
data class LMStudioUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0,
)
