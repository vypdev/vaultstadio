/**
 * Get AI Providers Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AIRepository
import com.vaultstadio.app.domain.model.AIProviderInfo
import org.koin.core.annotation.Factory

/**
 * Use case for getting available AI providers.
 */
interface GetAIProvidersUseCase {
    suspend operator fun invoke(): ApiResult<List<AIProviderInfo>>
}

@Factory(binds = [GetAIProvidersUseCase::class])
class GetAIProvidersUseCaseImpl(
    private val aiRepository: AIRepository,
) : GetAIProvidersUseCase {

    override suspend operator fun invoke(): ApiResult<List<AIProviderInfo>> =
        aiRepository.getProviders()
}
