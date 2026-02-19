/**
 * Configure AI Provider Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.AIRepository
import com.vaultstadio.app.domain.model.AIProviderType
/**
 * Use case for configuring an AI provider.
 */
interface ConfigureAIProviderUseCase {
    suspend operator fun invoke(
        type: AIProviderType,
        baseUrl: String,
        apiKey: String? = null,
        model: String? = null,
    ): Result<String>
}

class ConfigureAIProviderUseCaseImpl(
    private val aiRepository: AIRepository,
) : ConfigureAIProviderUseCase {

    override suspend operator fun invoke(
        type: AIProviderType,
        baseUrl: String,
        apiKey: String?,
        model: String?,
    ): Result<String> = aiRepository.configureProvider(type, baseUrl, apiKey, model)
}
