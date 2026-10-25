/**
 * Get Provider Models Use Case implementation
 */

package com.vaultstadio.app.data.ai.usecase

import com.vaultstadio.app.domain.ai.AIRepository
import com.vaultstadio.app.domain.ai.model.AIProviderType
import com.vaultstadio.app.domain.ai.usecase.GetProviderModelsUseCase

class GetProviderModelsUseCaseImpl(
    private val aiRepository: AIRepository,
) : GetProviderModelsUseCase {
    override suspend operator fun invoke(type: AIProviderType) = aiRepository.getProviderModels(type)
}
