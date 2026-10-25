/**
 * Configure AI Provider Use Case implementation
 */

package com.vaultstadio.app.data.ai.usecase

import com.vaultstadio.app.domain.ai.AIRepository
import com.vaultstadio.app.domain.ai.model.AIProviderType
import com.vaultstadio.app.domain.ai.usecase.ConfigureAIProviderUseCase

class ConfigureAIProviderUseCaseImpl(
    private val aiRepository: AIRepository,
) : ConfigureAIProviderUseCase {
    override suspend operator fun invoke(
        type: AIProviderType,
        baseUrl: String,
        apiKey: String?,
        model: String?,
    ) = aiRepository.configureProvider(type, baseUrl, apiKey, model)
}
