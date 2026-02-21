/**
 * Delete AI Provider Use Case implementation
 */

package com.vaultstadio.app.data.ai.usecase

import com.vaultstadio.app.domain.ai.AIRepository
import com.vaultstadio.app.domain.ai.model.AIProviderType
import com.vaultstadio.app.domain.ai.usecase.DeleteAIProviderUseCase

class DeleteAIProviderUseCaseImpl(
    private val aiRepository: AIRepository,
) : DeleteAIProviderUseCase {
    override suspend operator fun invoke(type: AIProviderType) = aiRepository.deleteProvider(type)
}
