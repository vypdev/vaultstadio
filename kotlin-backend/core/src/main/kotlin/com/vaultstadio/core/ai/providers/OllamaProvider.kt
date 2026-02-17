/**
 * Ollama AI Provider
 *
 * Supports local AI inference using Ollama.
 * Compatible with vision models like LLaVA, BakLLaVA.
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
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

/**
 * Ollama provider for local AI inference.
 */
class OllamaProvider(
    override val config: AIProviderConfig,
) : AIProvider {

    override val type = AIProviderType.OLLAMA

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
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

    override suspend fun isAvailable(): Boolean {
        return try {
            val response = client.get("${config.baseUrl}/api/tags")
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            logger.warn { "Ollama not available: ${e.message}" }
            false
        }
    }

    override suspend fun listModels(): Either<AIError, List<AIModel>> {
        return try {
            val response = client.get("${config.baseUrl}/api/tags")

            if (response.status != HttpStatusCode.OK) {
                return AIError.ProviderError("Failed to list models: ${response.status}").left()
            }

            val modelsResponse = response.body<OllamaModelsResponse>()

            modelsResponse.models.map { model ->
                AIModel(
                    id = model.name,
                    name = model.name,
                    provider = "ollama",
                    supportsVision = model.name.contains("llava", ignoreCase = true) ||
                        model.name.contains("vision", ignoreCase = true),
                    contextLength = 4096,
                    description = "Size: ${formatBytes(model.size)}",
                )
            }.right()
        } catch (e: Exception) {
            logger.error(e) { "Failed to list Ollama models" }
            AIError.ConnectionError("Failed to connect to Ollama: ${e.message}").left()
        }
    }

    override suspend fun chat(request: AIRequest): Either<AIError, AIResponse> {
        return try {
            val model = request.model ?: config.model

            // Convert messages to Ollama format
            val prompt = request.messages.joinToString("\n") { msg ->
                when (msg.role) {
                    "system" -> "[INST] <<SYS>>\n${msg.content}\n<</SYS>>\n[/INST]"
                    "user" -> msg.content
                    "assistant" -> msg.content
                    else -> msg.content
                }
            }

            // Check if any message has images
            val images = request.messages.flatMap { it.images ?: emptyList() }

            val ollamaRequest = if (images.isNotEmpty()) {
                OllamaGenerateRequest(
                    model = model,
                    prompt = prompt,
                    images = images,
                    stream = false,
                    options = OllamaOptions(
                        temperature = request.temperature ?: config.temperature,
                        numPredict = request.maxTokens ?: config.maxTokens,
                    ),
                )
            } else {
                OllamaGenerateRequest(
                    model = model,
                    prompt = prompt,
                    stream = false,
                    options = OllamaOptions(
                        temperature = request.temperature ?: config.temperature,
                        numPredict = request.maxTokens ?: config.maxTokens,
                    ),
                )
            }

            val response = client.post("${config.baseUrl}/api/generate") {
                contentType(ContentType.Application.Json)
                setBody(ollamaRequest)
            }

            if (response.status != HttpStatusCode.OK) {
                val error = response.bodyAsText()
                return AIError.ProviderError("Ollama error: $error").left()
            }

            val ollamaResponse = response.body<OllamaGenerateResponse>()

            AIResponse(
                content = ollamaResponse.response,
                model = model,
                promptTokens = ollamaResponse.promptEvalCount ?: 0,
                completionTokens = ollamaResponse.evalCount ?: 0,
                totalTokens = (ollamaResponse.promptEvalCount ?: 0) + (ollamaResponse.evalCount ?: 0),
                finishReason = if (ollamaResponse.done) "stop" else null,
            ).right()
        } catch (e: Exception) {
            logger.error(e) { "Ollama chat failed" }
            AIError.ProviderError("Ollama chat failed: ${e.message}").left()
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

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        val gb = mb / 1024.0
        return "%.1f GB".format(gb)
    }

    fun close() {
        client.close()
    }
}

// Ollama API Models
@Serializable
data class OllamaModelsResponse(
    val models: List<OllamaModel>,
)

@Serializable
data class OllamaModel(
    val name: String,
    val size: Long = 0,
    @SerialName("modified_at")
    val modifiedAt: String? = null,
)

@Serializable
data class OllamaGenerateRequest(
    val model: String,
    val prompt: String,
    val images: List<String>? = null,
    val stream: Boolean = false,
    val options: OllamaOptions? = null,
)

@Serializable
data class OllamaOptions(
    val temperature: Double? = null,
    @SerialName("num_predict")
    val numPredict: Int? = null,
)

@Serializable
data class OllamaGenerateResponse(
    val response: String,
    val done: Boolean = true,
    @SerialName("prompt_eval_count")
    val promptEvalCount: Int? = null,
    @SerialName("eval_count")
    val evalCount: Int? = null,
)
