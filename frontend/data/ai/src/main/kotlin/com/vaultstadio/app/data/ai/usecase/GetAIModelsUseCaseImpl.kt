/**
 * Get AI Models Use Case implementation
 */

package com.vaultstadio.app.data.ai.usecase

import com.vaultstadio.app.domain.ai.AIRepository
import com.vaultstadio.app.domain.ai.usecase.GetAIModelsUseCase

class GetAIModelsUseCaseImpl(
    private val aiRepository: AIRepository,
) : GetAIModelsUseCase {
    override suspend operator fun invoke() = aiRepository.getModels()
}
