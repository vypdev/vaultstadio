/**
 * Delete AI Provider Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.AIRepository
import com.vaultstadio.app.domain.model.AIProviderType
/**
 * Use case for deleting an AI provider configuration.
 */
interface DeleteAIProviderUseCase {
    suspend operator fun invoke(type: AIProviderType): Result<Unit>
}

class DeleteAIProviderUseCaseImpl(
    private val aiRepository: AIRepository,
) : DeleteAIProviderUseCase {

    override suspend operator fun invoke(type: AIProviderType): Result<Unit> =
        aiRepository.deleteProvider(type)
}
