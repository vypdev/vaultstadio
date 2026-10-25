/**
 * Get AI Provider Status Use Case implementation
 */

package com.vaultstadio.app.data.ai.usecase

import com.vaultstadio.app.domain.ai.AIRepository
import com.vaultstadio.app.domain.ai.model.AIProviderType
import com.vaultstadio.app.domain.ai.usecase.GetAIProviderStatusUseCase

class GetAIProviderStatusUseCaseImpl(
    private val aiRepository: AIRepository,
) : GetAIProviderStatusUseCase {
    override suspend operator fun invoke(type: AIProviderType) = aiRepository.getProviderStatus(type)
}
