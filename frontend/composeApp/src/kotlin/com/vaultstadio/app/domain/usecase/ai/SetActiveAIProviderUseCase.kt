/**
 * Set Active AI Provider Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.AIRepository
import com.vaultstadio.app.domain.model.AIProviderType
/**
 * Use case for setting the active AI provider.
 */
interface SetActiveAIProviderUseCase {
    suspend operator fun invoke(type: AIProviderType): Result<String>
}

class SetActiveAIProviderUseCaseImpl(
    private val aiRepository: AIRepository,
) : SetActiveAIProviderUseCase {

    override suspend operator fun invoke(type: AIProviderType): Result<String> =
        aiRepository.setActiveProvider(type)
}
