/**
 * AI API
 */

package com.vaultstadio.app.data.api

import com.vaultstadio.app.data.dto.ai.AIChatRequestDTO
import com.vaultstadio.app.data.dto.ai.AIChatResponseDTO
import com.vaultstadio.app.data.dto.ai.AIClassifyRequestDTO
import com.vaultstadio.app.data.dto.ai.AIDescribeRequestDTO
import com.vaultstadio.app.data.dto.ai.AIModelDTO
import com.vaultstadio.app.data.dto.ai.AIProviderConfigRequestDTO
import com.vaultstadio.app.data.dto.ai.AIProviderInfoDTO
import com.vaultstadio.app.data.dto.ai.AISummarizeRequestDTO
import com.vaultstadio.app.data.dto.ai.AITagRequestDTO
import com.vaultstadio.app.data.dto.ai.SetActiveProviderRequestDTO
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.BaseApi
import io.ktor.client.HttpClient
import org.koin.core.annotation.Single

@Single
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
