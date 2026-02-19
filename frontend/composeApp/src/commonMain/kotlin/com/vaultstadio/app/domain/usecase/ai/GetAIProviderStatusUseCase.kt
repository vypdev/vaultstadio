/**
 * Get AI Provider Status Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AIRepository
import com.vaultstadio.app.domain.model.AIProviderType
import org.koin.core.annotation.Factory

/**
 * Use case for checking the status of an AI provider.
 */
interface GetAIProviderStatusUseCase {
    suspend operator fun invoke(type: AIProviderType): ApiResult<Map<String, Boolean>>
}

@Factory(binds = [GetAIProviderStatusUseCase::class])
class GetAIProviderStatusUseCaseImpl(
    private val aiRepository: AIRepository,
) : GetAIProviderStatusUseCase {

    override suspend operator fun invoke(type: AIProviderType): ApiResult<Map<String, Boolean>> =
        aiRepository.getProviderStatus(type)
}
