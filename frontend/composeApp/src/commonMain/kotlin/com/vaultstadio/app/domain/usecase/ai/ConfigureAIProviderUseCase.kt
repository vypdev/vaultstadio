/**
 * Configure AI Provider Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AIRepository
import com.vaultstadio.app.domain.model.AIProviderType
import org.koin.core.annotation.Factory

/**
 * Use case for configuring an AI provider.
 */
interface ConfigureAIProviderUseCase {
    suspend operator fun invoke(
        type: AIProviderType,
        baseUrl: String,
        apiKey: String? = null,
        model: String? = null,
    ): ApiResult<String>
}

@Factory(binds = [ConfigureAIProviderUseCase::class])
class ConfigureAIProviderUseCaseImpl(
    private val aiRepository: AIRepository,
) : ConfigureAIProviderUseCase {

    override suspend operator fun invoke(
        type: AIProviderType,
        baseUrl: String,
        apiKey: String?,
        model: String?,
    ): ApiResult<String> = aiRepository.configureProvider(type, baseUrl, apiKey, model)
}
