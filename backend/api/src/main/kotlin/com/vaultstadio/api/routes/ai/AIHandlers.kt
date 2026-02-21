/**
 * AI request handlers.
 * Extracted from AIRoutes to keep the main file under the line limit.
 */

package com.vaultstadio.api.routes.ai

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.application.usecase.ai.AIServiceUseCase
import com.vaultstadio.core.ai.AIError
import com.vaultstadio.core.ai.AIMessage
import com.vaultstadio.core.ai.AIProviderConfig
import com.vaultstadio.core.ai.AIProviderType
import com.vaultstadio.core.ai.AIRequest
import com.vaultstadio.domain.auth.model.UserRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import org.koin.ktor.ext.get as koinGet

internal suspend fun handleGetProviders(call: ApplicationCall) {
    val aiUseCase: AIServiceUseCase = call.application.koinGet()
    val user = call.user!!
    if (user.role != UserRole.ADMIN) {
        call.respond(
            HttpStatusCode.Forbidden,
            ApiResponse<Unit>(success = false, error = ApiError("FORBIDDEN", "Admin access required")),
        )
        return
    }
    val activeProvider = aiUseCase.getActiveProvider()
    val providers = aiUseCase.getProviders().map { config ->
        AIProviderResponse(
            type = config.type.name,
            baseUrl = config.baseUrl,
            model = config.model,
            hasApiKey = !config.apiKey.isNullOrBlank(),
            timeout = config.timeout,
            maxTokens = config.maxTokens,
            temperature = config.temperature,
            enabled = config.enabled,
            isActive = config.type == activeProvider?.type,
        )
    }
    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = providers))
}

internal suspend fun handleConfigureProvider(call: ApplicationCall) {
    val aiUseCase: AIServiceUseCase = call.application.koinGet()
    val user = call.user!!
    if (user.role != UserRole.ADMIN) {
        call.respond(
            HttpStatusCode.Forbidden,
            ApiResponse<Unit>(success = false, error = ApiError("FORBIDDEN", "Admin access required")),
        )
        return
    }
    val request = call.receive<AIProviderConfigRequest>()
    val providerType = try {
        AIProviderType.valueOf(request.type.uppercase())
    } catch (e: Exception) {
        call.respond(
            HttpStatusCode.BadRequest,
            ApiResponse<Unit>(
                success = false,
                error = ApiError("INVALID_TYPE", "Invalid provider type: ${request.type}"),
            ),
        )
        return
    }
    val config = AIProviderConfig(
        type = providerType,
        baseUrl = request.baseUrl,
        apiKey = request.apiKey,
        model = request.model,
        timeout = request.timeout,
        maxTokens = request.maxTokens,
        temperature = request.temperature,
        enabled = request.enabled,
    )
    aiUseCase.configureProvider(config).fold(
        { error ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, error = ApiError("CONFIG_ERROR", error.errorMessage)),
            )
        },
        {
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = "Provider configured"))
        },
    )
}

internal suspend fun handleSetActiveProvider(call: ApplicationCall) {
    val aiUseCase: AIServiceUseCase = call.application.koinGet()
    val user = call.user!!
    if (user.role != UserRole.ADMIN) {
        call.respond(
            HttpStatusCode.Forbidden,
            ApiResponse<Unit>(success = false, error = ApiError("FORBIDDEN", "Admin access required")),
        )
        return
    }
    val request = call.receive<SetActiveProviderRequest>()
    val providerType = try {
        AIProviderType.valueOf(request.type.uppercase())
    } catch (e: Exception) {
        call.respond(
            HttpStatusCode.BadRequest,
            ApiResponse<Unit>(success = false, error = ApiError("INVALID_TYPE", "Invalid provider type")),
        )
        return
    }
    aiUseCase.setActiveProvider(providerType).fold(
        { error ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, error = ApiError("SET_ACTIVE_ERROR", error.errorMessage)),
            )
        },
        {
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = "Active provider set"))
        },
    )
}

internal suspend fun handleDeleteProvider(call: ApplicationCall) {
    val aiUseCase: AIServiceUseCase = call.application.koinGet()
    val user = call.user!!
    if (user.role != UserRole.ADMIN) {
        call.respond(
            HttpStatusCode.Forbidden,
            ApiResponse<Unit>(success = false, error = ApiError("FORBIDDEN", "Admin access required")),
        )
        return
    }
    val typeParam = call.parameters["type"]!!
    val providerType = try {
        AIProviderType.valueOf(typeParam.uppercase())
    } catch (e: Exception) {
        call.respond(
            HttpStatusCode.BadRequest,
            ApiResponse<Unit>(success = false, error = ApiError("INVALID_TYPE", "Invalid provider type")),
        )
        return
    }
    aiUseCase.removeProvider(providerType).fold(
        { error ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, error = ApiError("DELETE_ERROR", error.errorMessage)),
            )
        },
        {
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = "Provider removed"))
        },
    )
}

internal suspend fun handleGetProviderStatus(call: ApplicationCall) {
    val aiUseCase: AIServiceUseCase = call.application.koinGet()
    val typeParam = call.parameters["type"]!!
    val providerType = try {
        AIProviderType.valueOf(typeParam.uppercase())
    } catch (e: Exception) {
        call.respond(
            HttpStatusCode.BadRequest,
            ApiResponse<Unit>(success = false, error = ApiError("INVALID_TYPE", "Invalid provider type")),
        )
        return
    }
    val available = aiUseCase.isProviderAvailable(providerType)
    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = mapOf("available" to available)))
}

internal suspend fun handleListModels(call: ApplicationCall) {
    val aiUseCase: AIServiceUseCase = call.application.koinGet()
    aiUseCase.listModels().fold(
        { error ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, error = ApiError("LIST_MODELS_ERROR", error.errorMessage)),
            )
        },
        { models ->
            val response = models.map { model ->
                AIModelResponse(
                    id = model.id,
                    name = model.name,
                    provider = model.provider,
                    supportsVision = model.supportsVision,
                    contextLength = model.contextLength,
                    description = model.description,
                )
            }
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = response))
        },
    )
}

internal suspend fun handleListModelsByType(call: ApplicationCall) {
    val aiUseCase: AIServiceUseCase = call.application.koinGet()
    val typeParam = call.parameters["type"]!!
    val providerType = try {
        AIProviderType.valueOf(typeParam.uppercase())
    } catch (e: Exception) {
        call.respond(
            HttpStatusCode.BadRequest,
            ApiResponse<Unit>(success = false, error = ApiError("INVALID_TYPE", "Invalid provider type")),
        )
        return
    }
    aiUseCase.listModels(providerType).fold(
        { error ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, error = ApiError("LIST_MODELS_ERROR", error.errorMessage)),
            )
        },
        { models ->
            val response = models.map { model ->
                AIModelResponse(
                    id = model.id,
                    name = model.name,
                    provider = model.provider,
                    supportsVision = model.supportsVision,
                    contextLength = model.contextLength,
                    description = model.description,
                )
            }
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = response))
        },
    )
}

internal suspend fun handleChat(call: ApplicationCall) {
    val aiUseCase: AIServiceUseCase = call.application.koinGet()
    val request = call.receive<AIChatRequest>()
    val aiRequest = AIRequest(
        messages = request.messages.map { msg ->
            AIMessage(role = msg.role, content = msg.content, images = msg.images)
        },
        model = request.model,
        maxTokens = request.maxTokens,
        temperature = request.temperature,
    )
    aiUseCase.chat(aiRequest).fold(
        { error ->
            val statusCode = when (error) {
                is AIError.AuthenticationError -> HttpStatusCode.Unauthorized
                is AIError.RateLimitError -> HttpStatusCode.TooManyRequests
                is AIError.ModelNotFoundError -> HttpStatusCode.NotFound
                else -> HttpStatusCode.BadRequest
            }
            call.respond(
                statusCode,
                ApiResponse<Unit>(success = false, error = ApiError("AI_ERROR", error.errorMessage)),
            )
        },
        { response ->
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = AIChatResponse(
                        content = response.content,
                        model = response.model,
                        promptTokens = response.promptTokens,
                        completionTokens = response.completionTokens,
                        totalTokens = response.totalTokens,
                    ),
                ),
            )
        },
    )
}

internal suspend fun handleVision(call: ApplicationCall) {
    val aiUseCase: AIServiceUseCase = call.application.koinGet()
    val request = call.receive<AIVisionRequest>()
    aiUseCase.vision(
        prompt = request.prompt,
        imageBase64 = request.imageBase64,
        mimeType = request.mimeType,
    ).fold(
        { error ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, error = ApiError("VISION_ERROR", error.errorMessage)),
            )
        },
        { response ->
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = AIChatResponse(
                        content = response.content,
                        model = response.model,
                        promptTokens = response.promptTokens,
                        completionTokens = response.completionTokens,
                        totalTokens = response.totalTokens,
                    ),
                ),
            )
        },
    )
}

internal suspend fun handleDescribe(call: ApplicationCall) {
    val aiUseCase: AIServiceUseCase = call.application.koinGet()
    val request = call.receive<AIDescribeRequest>()
    aiUseCase.describeImage(request.imageBase64).fold(
        { error ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, error = ApiError("DESCRIBE_ERROR", error.errorMessage)),
            )
        },
        { description ->
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = mapOf("description" to description)))
        },
    )
}

internal suspend fun handleTag(call: ApplicationCall) {
    val aiUseCase: AIServiceUseCase = call.application.koinGet()
    val request = call.receive<AITagRequest>()
    aiUseCase.tagImage(request.imageBase64).fold(
        { error ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, error = ApiError("TAG_ERROR", error.errorMessage)),
            )
        },
        { tags ->
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = mapOf("tags" to tags)))
        },
    )
}

internal suspend fun handleClassify(call: ApplicationCall) {
    val aiUseCase: AIServiceUseCase = call.application.koinGet()
    val request = call.receive<AIClassifyRequest>()
    if (request.categories.isEmpty()) {
        call.respond(
            HttpStatusCode.BadRequest,
            ApiResponse<Unit>(
                success = false,
                error = ApiError("INVALID_REQUEST", "Categories cannot be empty"),
            ),
        )
        return
    }
    aiUseCase.classify(request.content, request.categories).fold(
        { error ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, error = ApiError("CLASSIFY_ERROR", error.errorMessage)),
            )
        },
        { category ->
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(success = true, data = mapOf("category" to category)),
            )
        },
    )
}

internal suspend fun handleSummarize(call: ApplicationCall) {
    val aiUseCase: AIServiceUseCase = call.application.koinGet()
    val request = call.receive<AISummarizeRequest>()
    aiUseCase.summarize(request.text, request.maxLength).fold(
        { error ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, error = ApiError("SUMMARIZE_ERROR", error.errorMessage)),
            )
        },
        { summary ->
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = mapOf("summary" to summary)))
        },
    )
}
