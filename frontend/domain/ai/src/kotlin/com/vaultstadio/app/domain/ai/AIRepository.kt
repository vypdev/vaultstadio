/**
 * Repository interface for AI operations.
 */

package com.vaultstadio.app.domain.ai

import com.vaultstadio.app.domain.ai.model.AIChatMessage
import com.vaultstadio.app.domain.ai.model.AIChatResponse
import com.vaultstadio.app.domain.ai.model.AIModel
import com.vaultstadio.app.domain.ai.model.AIProviderInfo
import com.vaultstadio.app.domain.ai.model.AIProviderType
import com.vaultstadio.app.domain.result.Result

interface AIRepository {
    suspend fun getProviders(): Result<List<AIProviderInfo>>
    suspend fun configureProvider(
        type: AIProviderType,
        baseUrl: String,
        apiKey: String? = null,
        model: String? = null,
    ): Result<String>
    suspend fun setActiveProvider(type: AIProviderType): Result<String>
    suspend fun deleteProvider(type: AIProviderType): Result<Unit>
    suspend fun getProviderStatus(type: AIProviderType): Result<Map<String, Boolean>>
    suspend fun getModels(): Result<List<AIModel>>
    suspend fun getProviderModels(type: AIProviderType): Result<List<AIModel>>
    suspend fun chat(
        messages: List<AIChatMessage>,
        model: String? = null,
        maxTokens: Int? = null,
        temperature: Double? = null,
    ): Result<AIChatResponse>
    suspend fun describeImage(imageBase64: String, mimeType: String = "image/jpeg"): Result<String>
    suspend fun tagImage(imageBase64: String, mimeType: String = "image/jpeg"): Result<List<String>>
    suspend fun classify(content: String, categories: List<String>): Result<String>
    suspend fun summarize(text: String, maxLength: Int = 200): Result<String>
}
