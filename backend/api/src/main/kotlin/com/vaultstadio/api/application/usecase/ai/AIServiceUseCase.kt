/**
 * AI Service Use Case
 *
 * Application use case that wraps AIService for provider and model operations.
 * Routes depend on this use case instead of AIService directly.
 */

package com.vaultstadio.api.application.usecase.ai

import arrow.core.Either
import com.vaultstadio.core.ai.AIError
import com.vaultstadio.core.ai.AIModel
import com.vaultstadio.core.ai.AIProviderConfig
import com.vaultstadio.core.ai.AIProviderType
import com.vaultstadio.core.ai.AIRequest
import com.vaultstadio.core.ai.AIResponse
import com.vaultstadio.core.ai.AIService

/**
 * Use case exposing AI operations (delegates to [AIService]).
 */
interface AIServiceUseCase {

    fun getProviders(): List<AIProviderConfig>

    fun getActiveProvider(): AIProviderConfig?

    suspend fun setActiveProvider(type: AIProviderType): Either<AIError, Unit>

    suspend fun configureProvider(config: AIProviderConfig): Either<AIError, Unit>

    suspend fun removeProvider(type: AIProviderType): Either<AIError, Unit>

    suspend fun isProviderAvailable(type: AIProviderType): Boolean

    suspend fun listModels(): Either<AIError, List<AIModel>>

    suspend fun listModels(type: AIProviderType): Either<AIError, List<AIModel>>

    suspend fun chat(request: AIRequest): Either<AIError, AIResponse>

    suspend fun vision(
        prompt: String,
        imageBase64: String,
        mimeType: String = "image/jpeg",
    ): Either<AIError, AIResponse>

    suspend fun describeImage(imageBase64: String): Either<AIError, String>

    suspend fun tagImage(imageBase64: String): Either<AIError, List<String>>

    suspend fun classify(content: String, categories: List<String>): Either<AIError, String>

    suspend fun summarize(text: String, maxLength: Int = 200): Either<AIError, String>
}

/**
 * Default implementation delegating to [AIService].
 */
class AIServiceUseCaseImpl(
    private val aiService: AIService,
) : AIServiceUseCase {

    override fun getProviders(): List<AIProviderConfig> = aiService.getProviders()

    override fun getActiveProvider(): AIProviderConfig? = aiService.getActiveProvider()

    override suspend fun setActiveProvider(type: AIProviderType): Either<AIError, Unit> =
        aiService.setActiveProvider(type)

    override suspend fun configureProvider(config: AIProviderConfig): Either<AIError, Unit> =
        aiService.configureProvider(config)

    override suspend fun removeProvider(type: AIProviderType): Either<AIError, Unit> =
        aiService.removeProvider(type)

    override suspend fun isProviderAvailable(type: AIProviderType): Boolean =
        aiService.isProviderAvailable(type)

    override suspend fun listModels(): Either<AIError, List<AIModel>> = aiService.listModels()

    override suspend fun listModels(type: AIProviderType): Either<AIError, List<AIModel>> =
        aiService.listModels(type)

    override suspend fun chat(request: AIRequest): Either<AIError, AIResponse> = aiService.chat(request)

    override suspend fun vision(
        prompt: String,
        imageBase64: String,
        mimeType: String,
    ): Either<AIError, AIResponse> =
        aiService.vision(prompt, imageBase64, mimeType)

    override suspend fun describeImage(imageBase64: String): Either<AIError, String> =
        aiService.describeImage(imageBase64)

    override suspend fun tagImage(imageBase64: String): Either<AIError, List<String>> =
        aiService.tagImage(imageBase64)

    override suspend fun classify(content: String, categories: List<String>): Either<AIError, String> =
        aiService.classify(content, categories)

    override suspend fun summarize(text: String, maxLength: Int): Either<AIError, String> =
        aiService.summarize(text, maxLength)
}
