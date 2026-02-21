/**
 * AI Service
 */

package com.vaultstadio.app.data.ai.service

import com.vaultstadio.app.data.ai.api.AIApi
import com.vaultstadio.app.data.ai.dto.AIChatRequestDTO
import com.vaultstadio.app.data.ai.dto.AIClassifyRequestDTO
import com.vaultstadio.app.data.ai.dto.AIDescribeRequestDTO
import com.vaultstadio.app.data.ai.dto.AIProviderConfigRequestDTO
import com.vaultstadio.app.data.ai.dto.AISummarizeRequestDTO
import com.vaultstadio.app.data.ai.dto.AITagRequestDTO
import com.vaultstadio.app.data.ai.mapper.toDTO
import com.vaultstadio.app.data.ai.mapper.toDomain
import com.vaultstadio.app.data.ai.mapper.toModelList
import com.vaultstadio.app.data.ai.mapper.toProviderList
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.ai.model.AIChatMessage
import com.vaultstadio.app.domain.ai.model.AIChatResponse
import com.vaultstadio.app.domain.ai.model.AIModel
import com.vaultstadio.app.domain.ai.model.AIProviderInfo
import com.vaultstadio.app.domain.ai.model.AIProviderType

class AIService(private val aiApi: AIApi) {

    suspend fun getProviders(): ApiResult<List<AIProviderInfo>> =
        aiApi.getProviders().map { it.toProviderList() }

    suspend fun configureProvider(
        type: AIProviderType,
        baseUrl: String,
        apiKey: String? = null,
        model: String? = null,
    ): ApiResult<String> =
        aiApi.configureProvider(AIProviderConfigRequestDTO(type.name, baseUrl, apiKey, model))

    suspend fun setActiveProvider(type: AIProviderType): ApiResult<String> =
        aiApi.setActiveProvider(type.name)

    suspend fun deleteProvider(type: AIProviderType): ApiResult<Unit> =
        aiApi.deleteProvider(type.name)

    suspend fun getProviderStatus(type: AIProviderType): ApiResult<Map<String, Boolean>> =
        aiApi.getProviderStatus(type.name)

    suspend fun getModels(): ApiResult<List<AIModel>> =
        aiApi.getModels().map { it.toModelList() }

    suspend fun getProviderModels(type: AIProviderType): ApiResult<List<AIModel>> =
        aiApi.getProviderModels(type.name).map { it.toModelList() }

    suspend fun chat(
        messages: List<AIChatMessage>,
        model: String? = null,
        maxTokens: Int? = null,
        temperature: Double? = null,
    ): ApiResult<AIChatResponse> =
        aiApi.chat(AIChatRequestDTO(messages.map { it.toDTO() }, model, maxTokens, temperature))
            .map { it.toDomain() }

    suspend fun describeImage(imageBase64: String, mimeType: String = "image/jpeg"): ApiResult<String> =
        aiApi.describeImage(AIDescribeRequestDTO(imageBase64, mimeType))
            .map { it["description"] ?: "" }

    suspend fun tagImage(imageBase64: String, mimeType: String = "image/jpeg"): ApiResult<List<String>> =
        aiApi.tagImage(AITagRequestDTO(imageBase64, mimeType))
            .map { it["tags"] ?: emptyList() }

    suspend fun classify(content: String, categories: List<String>): ApiResult<String> =
        aiApi.classify(AIClassifyRequestDTO(content, categories))
            .map { it["category"] ?: "" }

    suspend fun summarize(text: String, maxLength: Int = 200): ApiResult<String> =
        aiApi.summarize(AISummarizeRequestDTO(text, maxLength))
            .map { it["summary"] ?: "" }
}
