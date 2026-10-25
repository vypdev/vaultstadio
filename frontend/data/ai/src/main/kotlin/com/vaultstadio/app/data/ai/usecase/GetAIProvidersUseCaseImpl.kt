/**
 * Get AI Providers Use Case implementation
 */

package com.vaultstadio.app.data.ai.usecase

import com.vaultstadio.app.domain.ai.AIRepository
import com.vaultstadio.app.domain.ai.usecase.GetAIProvidersUseCase

class GetAIProvidersUseCaseImpl(
    private val aiRepository: AIRepository,
) : GetAIProvidersUseCase {
    override suspend operator fun invoke() = aiRepository.getProviders()
}
