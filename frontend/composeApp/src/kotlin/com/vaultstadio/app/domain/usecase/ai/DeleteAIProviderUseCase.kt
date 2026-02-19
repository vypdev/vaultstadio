/**
 * Delete AI Provider Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.AIRepository
import com.vaultstadio.app.domain.model.AIProviderType
import org.koin.core.annotation.Factory

/**
 * Use case for deleting an AI provider configuration.
 */
interface DeleteAIProviderUseCase {
    suspend operator fun invoke(type: AIProviderType): Result<Unit>
}

@Factory(binds = [DeleteAIProviderUseCase::class])
class DeleteAIProviderUseCaseImpl(
    private val aiRepository: AIRepository,
) : DeleteAIProviderUseCase {

    override suspend operator fun invoke(type: AIProviderType): Result<Unit> =
        aiRepository.deleteProvider(type)
}
