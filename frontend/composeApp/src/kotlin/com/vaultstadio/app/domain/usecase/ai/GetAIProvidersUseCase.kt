/**
 * Get AI Providers Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.AIRepository
import com.vaultstadio.app.domain.model.AIProviderInfo
/**
 * Use case for getting available AI providers.
 */
interface GetAIProvidersUseCase {
    suspend operator fun invoke(): Result<List<AIProviderInfo>>
}

class GetAIProvidersUseCaseImpl(
    private val aiRepository: AIRepository,
) : GetAIProvidersUseCase {

    override suspend operator fun invoke(): Result<List<AIProviderInfo>> =
        aiRepository.getProviders()
}
