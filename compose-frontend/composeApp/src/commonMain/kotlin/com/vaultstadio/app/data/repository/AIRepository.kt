/**
 * AI Repository
 */

package com.vaultstadio.app.data.repository

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.service.AIService
import com.vaultstadio.app.domain.model.AIChatMessage
import com.vaultstadio.app.domain.model.AIChatResponse
import com.vaultstadio.app.domain.model.AIModel
import com.vaultstadio.app.domain.model.AIProviderInfo
import com.vaultstadio.app.domain.model.AIProviderType
import org.koin.core.annotation.Single

/**
 * Repository interface for AI operations.
 */
interface AIRepository {
    suspend fun getProviders(): ApiResult<List<AIProviderInfo>>
    suspend fun configureProvider(
        type: AIProviderType,
        baseUrl: String,
        apiKey: String? = null,
        model: String? = null,
    ): ApiResult<String>
    suspend fun setActiveProvider(type: AIProviderType): ApiResult<String>
    suspend fun deleteProvider(type: AIProviderType): ApiResult<Unit>
    suspend fun getProviderStatus(type: AIProviderType): ApiResult<Map<String, Boolean>>
    suspend fun getModels(): ApiResult<List<AIModel>>
    suspend fun getProviderModels(type: AIProviderType): ApiResult<List<AIModel>>
    suspend fun chat(
        messages: List<AIChatMessage>,
        model: String? = null,
        maxTokens: Int? = null,
        temperature: Double? = null,
    ): ApiResult<AIChatResponse>
    suspend fun describeImage(imageBase64: String, mimeType: String = "image/jpeg"): ApiResult<String>
    suspend fun tagImage(imageBase64: String, mimeType: String = "image/jpeg"): ApiResult<List<String>>
    suspend fun classify(content: String, categories: List<String>): ApiResult<String>
    suspend fun summarize(text: String, maxLength: Int = 200): ApiResult<String>
}

@Single(binds = [AIRepository::class])
class AIRepositoryImpl(
    private val aiService: AIService,
) : AIRepository {

    override suspend fun getProviders(): ApiResult<List<AIProviderInfo>> = aiService.getProviders()

    override suspend fun configureProvider(
        type: AIProviderType,
        baseUrl: String,
        apiKey: String?,
        model: String?,
    ): ApiResult<String> = aiService.configureProvider(type, baseUrl, apiKey, model)

    override suspend fun setActiveProvider(type: AIProviderType): ApiResult<String> =
        aiService.setActiveProvider(type)

    override suspend fun deleteProvider(type: AIProviderType): ApiResult<Unit> =
        aiService.deleteProvider(type)

    override suspend fun getProviderStatus(type: AIProviderType): ApiResult<Map<String, Boolean>> =
        aiService.getProviderStatus(type)

    override suspend fun getModels(): ApiResult<List<AIModel>> = aiService.getModels()

    override suspend fun getProviderModels(type: AIProviderType): ApiResult<List<AIModel>> =
        aiService.getProviderModels(type)

    override suspend fun chat(
        messages: List<AIChatMessage>,
        model: String?,
        maxTokens: Int?,
        temperature: Double?,
    ): ApiResult<AIChatResponse> = aiService.chat(messages, model, maxTokens, temperature)

    override suspend fun describeImage(imageBase64: String, mimeType: String): ApiResult<String> =
        aiService.describeImage(imageBase64, mimeType)

    override suspend fun tagImage(imageBase64: String, mimeType: String): ApiResult<List<String>> =
        aiService.tagImage(imageBase64, mimeType)

    override suspend fun classify(content: String, categories: List<String>): ApiResult<String> =
        aiService.classify(content, categories)

    override suspend fun summarize(text: String, maxLength: Int): ApiResult<String> =
        aiService.summarize(text, maxLength)
}
