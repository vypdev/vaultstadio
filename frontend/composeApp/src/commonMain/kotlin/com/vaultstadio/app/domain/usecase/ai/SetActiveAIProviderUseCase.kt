/**
 * Set Active AI Provider Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AIRepository
import com.vaultstadio.app.domain.model.AIProviderType
import org.koin.core.annotation.Factory

/**
 * Use case for setting the active AI provider.
 */
interface SetActiveAIProviderUseCase {
    suspend operator fun invoke(type: AIProviderType): ApiResult<String>
}

@Factory(binds = [SetActiveAIProviderUseCase::class])
class SetActiveAIProviderUseCaseImpl(
    private val aiRepository: AIRepository,
) : SetActiveAIProviderUseCase {

    override suspend operator fun invoke(type: AIProviderType): ApiResult<String> =
        aiRepository.setActiveProvider(type)
}
