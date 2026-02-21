/**
 * AI API
 */

package com.vaultstadio.app.data.ai.api

import com.vaultstadio.app.data.ai.dto.AIChatRequestDTO
import com.vaultstadio.app.data.ai.dto.AIChatResponseDTO
import com.vaultstadio.app.data.ai.dto.AIClassifyRequestDTO
import com.vaultstadio.app.data.ai.dto.AIDescribeRequestDTO
import com.vaultstadio.app.data.ai.dto.AIModelDTO
import com.vaultstadio.app.data.ai.dto.AIProviderConfigRequestDTO
import com.vaultstadio.app.data.ai.dto.AIProviderInfoDTO
import com.vaultstadio.app.data.ai.dto.AISummarizeRequestDTO
import com.vaultstadio.app.data.ai.dto.AITagRequestDTO
import com.vaultstadio.app.data.ai.dto.SetActiveProviderRequestDTO
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.BaseApi
import io.ktor.client.HttpClient

class AIApi(client: HttpClient) : BaseApi(client) {

    suspend fun getProviders(): ApiResult<List<AIProviderInfoDTO>> = get("/api/v1/ai/providers")

    suspend fun configureProvider(request: AIProviderConfigRequestDTO): ApiResult<String> =
        post("/api/v1/ai/providers", request)

    suspend fun setActiveProvider(type: String): ApiResult<String> =
        post("/api/v1/ai/providers/active", SetActiveProviderRequestDTO(type))

    suspend fun deleteProvider(type: String): ApiResult<Unit> =
        delete("/api/v1/ai/providers/${type.lowercase()}")

    suspend fun getProviderStatus(type: String): ApiResult<Map<String, Boolean>> =
        get("/api/v1/ai/providers/${type.lowercase()}/status")

    suspend fun getModels(): ApiResult<List<AIModelDTO>> = get("/api/v1/ai/models")

    suspend fun getProviderModels(type: String): ApiResult<List<AIModelDTO>> =
        get("/api/v1/ai/providers/${type.lowercase()}/models")

    suspend fun chat(request: AIChatRequestDTO): ApiResult<AIChatResponseDTO> =
        post("/api/v1/ai/chat", request)

    suspend fun describeImage(request: AIDescribeRequestDTO): ApiResult<Map<String, String>> =
        post("/api/v1/ai/describe", request)

    suspend fun tagImage(request: AITagRequestDTO): ApiResult<Map<String, List<String>>> =
        post("/api/v1/ai/tag", request)

    suspend fun classify(request: AIClassifyRequestDTO): ApiResult<Map<String, String>> =
        post("/api/v1/ai/classify", request)

    suspend fun summarize(request: AISummarizeRequestDTO): ApiResult<Map<String, String>> =
        post("/api/v1/ai/summarize", request)
}
