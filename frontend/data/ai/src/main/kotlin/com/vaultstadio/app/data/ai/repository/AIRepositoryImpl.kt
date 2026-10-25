/**
 * AI Repository implementation
 */

package com.vaultstadio.app.data.ai.repository

import com.vaultstadio.app.data.ai.service.AIService
import com.vaultstadio.app.data.network.mapper.toResult
import com.vaultstadio.app.domain.ai.AIRepository
import com.vaultstadio.app.domain.ai.model.AIChatMessage
import com.vaultstadio.app.domain.ai.model.AIChatResponse
import com.vaultstadio.app.domain.ai.model.AIModel
import com.vaultstadio.app.domain.ai.model.AIProviderInfo
import com.vaultstadio.app.domain.ai.model.AIProviderType
import com.vaultstadio.app.domain.result.Result

class AIRepositoryImpl(
    private val aiService: AIService,
) : AIRepository {

    override suspend fun getProviders(): Result<List<AIProviderInfo>> =
        aiService.getProviders().toResult()

    override suspend fun configureProvider(
        type: AIProviderType,
        baseUrl: String,
        apiKey: String?,
        model: String?,
    ): Result<String> =
        aiService.configureProvider(type, baseUrl, apiKey, model).toResult()

    override suspend fun setActiveProvider(type: AIProviderType): Result<String> =
        aiService.setActiveProvider(type).toResult()

    override suspend fun deleteProvider(type: AIProviderType): Result<Unit> =
        aiService.deleteProvider(type).toResult()

    override suspend fun getProviderStatus(type: AIProviderType): Result<Map<String, Boolean>> =
        aiService.getProviderStatus(type).toResult()

    override suspend fun getModels(): Result<List<AIModel>> =
        aiService.getModels().toResult()

    override suspend fun getProviderModels(type: AIProviderType): Result<List<AIModel>> =
        aiService.getProviderModels(type).toResult()

    override suspend fun chat(
        messages: List<AIChatMessage>,
        model: String?,
        maxTokens: Int?,
        temperature: Double?,
    ): Result<AIChatResponse> =
        aiService.chat(messages, model, maxTokens, temperature).toResult()

    override suspend fun describeImage(imageBase64: String, mimeType: String): Result<String> =
        aiService.describeImage(imageBase64, mimeType).toResult()

    override suspend fun tagImage(imageBase64: String, mimeType: String): Result<List<String>> =
        aiService.tagImage(imageBase64, mimeType).toResult()

    override suspend fun classify(content: String, categories: List<String>): Result<String> =
        aiService.classify(content, categories).toResult()

    override suspend fun summarize(text: String, maxLength: Int): Result<String> =
        aiService.summarize(text, maxLength).toResult()
}
