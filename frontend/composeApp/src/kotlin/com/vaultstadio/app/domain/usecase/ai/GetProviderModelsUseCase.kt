/**
 * Get Provider Models Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.AIRepository
import com.vaultstadio.app.domain.model.AIModel
import com.vaultstadio.app.domain.model.AIProviderType
import org.koin.core.annotation.Factory

/**
 * Use case for getting models available for a specific AI provider.
 */
interface GetProviderModelsUseCase {
    suspend operator fun invoke(type: AIProviderType): Result<List<AIModel>>
}

@Factory(binds = [GetProviderModelsUseCase::class])
class GetProviderModelsUseCaseImpl(
    private val aiRepository: AIRepository,
) : GetProviderModelsUseCase {

    override suspend operator fun invoke(type: AIProviderType): Result<List<AIModel>> =
        aiRepository.getProviderModels(type)
}
