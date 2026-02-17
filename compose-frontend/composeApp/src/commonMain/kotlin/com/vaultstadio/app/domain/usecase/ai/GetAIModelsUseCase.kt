/**
 * Get AI Models Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AIRepository
import com.vaultstadio.app.domain.model.AIModel
import org.koin.core.annotation.Factory

/**
 * Use case for getting available AI models.
 */
interface GetAIModelsUseCase {
    suspend operator fun invoke(): ApiResult<List<AIModel>>
}

@Factory(binds = [GetAIModelsUseCase::class])
class GetAIModelsUseCaseImpl(
    private val aiRepository: AIRepository,
) : GetAIModelsUseCase {

    override suspend operator fun invoke(): ApiResult<List<AIModel>> =
        aiRepository.getModels()
}
