/**
 * VaultStadio AI Routes
 *
 * API endpoints for AI functionality:
 * - Provider configuration
 * - Model listing
 * - Image analysis
 * - Content classification
 */

package com.vaultstadio.api.routes.ai

import com.vaultstadio.core.ai.AIService
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.get as koinGet

@Serializable
data class AIProviderConfigRequest(
    val type: String,
    val baseUrl: String,
    val apiKey: String? = null,
    val model: String,
    val timeout: Long = 120000,
    val maxTokens: Int = 1024,
    val temperature: Double = 0.7,
    val enabled: Boolean = true,
)

@Serializable
data class AIProviderResponse(
    val type: String,
    val baseUrl: String,
    val model: String,
    val hasApiKey: Boolean,
    val timeout: Long,
    val maxTokens: Int,
    val temperature: Double,
    val enabled: Boolean,
    val isActive: Boolean,
)

@Serializable
data class AIModelResponse(
    val id: String,
    val name: String,
    val provider: String,
    val supportsVision: Boolean,
    val contextLength: Int,
    val description: String?,
)

@Serializable
data class AIChatRequest(
    val messages: List<AIChatMessage>,
    val model: String? = null,
    val maxTokens: Int? = null,
    val temperature: Double? = null,
)

@Serializable
data class AIChatMessage(
    val role: String,
    val content: String,
    val images: List<String>? = null,
)

@Serializable
data class AIChatResponse(
    val content: String,
    val model: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)

@Serializable
data class AIVisionRequest(
    val prompt: String,
    val imageBase64: String,
    val mimeType: String = "image/jpeg",
)

@Serializable
data class AIDescribeRequest(
    val imageBase64: String,
)

@Serializable
data class AITagRequest(
    val imageBase64: String,
)

@Serializable
data class AIClassifyRequest(
    val content: String,
    val categories: List<String>,
)

@Serializable
data class AISummarizeRequest(
    val text: String,
    val maxLength: Int = 200,
)

@Serializable
data class SetActiveProviderRequest(
    val type: String,
)

fun Route.aiRoutes() {
    route("/ai") {
        get("/providers") {
            handleGetProviders(call, call.application.koinGet<AIService>())
        }
        post("/providers") {
            handleConfigureProvider(call, call.application.koinGet<AIService>())
        }
        post("/providers/active") {
            handleSetActiveProvider(call, call.application.koinGet<AIService>())
        }
        delete("/providers/{type}") {
            handleDeleteProvider(call, call.application.koinGet<AIService>())
        }
        get("/providers/{type}/status") {
            handleGetProviderStatus(call, call.application.koinGet<AIService>())
        }
        get("/models") {
            handleListModels(call, call.application.koinGet<AIService>())
        }
        get("/providers/{type}/models") {
            handleListModelsByType(call, call.application.koinGet<AIService>())
        }
        post("/chat") {
            handleChat(call, call.application.koinGet<AIService>())
        }
        post("/vision") {
            handleVision(call, call.application.koinGet<AIService>())
        }
        post("/describe") {
            handleDescribe(call, call.application.koinGet<AIService>())
        }
        post("/tag") {
            handleTag(call, call.application.koinGet<AIService>())
        }
        post("/classify") {
            handleClassify(call, call.application.koinGet<AIService>())
        }
        post("/summarize") {
            handleSummarize(call, call.application.koinGet<AIService>())
        }
    }
}
