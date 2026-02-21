/**
 * Set Active AI Provider Use Case implementation
 */

package com.vaultstadio.app.data.ai.usecase

import com.vaultstadio.app.domain.ai.AIRepository
import com.vaultstadio.app.domain.ai.model.AIProviderType
import com.vaultstadio.app.domain.ai.usecase.SetActiveAIProviderUseCase

class SetActiveAIProviderUseCaseImpl(
    private val aiRepository: AIRepository,
) : SetActiveAIProviderUseCase {
    override suspend operator fun invoke(type: AIProviderType) = aiRepository.setActiveProvider(type)
}
