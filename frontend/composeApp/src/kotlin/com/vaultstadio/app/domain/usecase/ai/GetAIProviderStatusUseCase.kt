/**
 * Get AI Provider Status Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.AIRepository
import com.vaultstadio.app.domain.model.AIProviderType
/**
 * Use case for checking the status of an AI provider.
 */
interface GetAIProviderStatusUseCase {
    suspend operator fun invoke(type: AIProviderType): Result<Map<String, Boolean>>
}

class GetAIProviderStatusUseCaseImpl(
    private val aiRepository: AIRepository,
) : GetAIProviderStatusUseCase {

    override suspend operator fun invoke(type: AIProviderType): Result<Map<String, Boolean>> =
        aiRepository.getProviderStatus(type)
}
