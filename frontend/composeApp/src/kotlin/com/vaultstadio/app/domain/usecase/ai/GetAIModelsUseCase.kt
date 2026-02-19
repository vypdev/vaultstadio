/**
 * Get AI Models Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.AIRepository
import com.vaultstadio.app.domain.model.AIModel
/**
 * Use case for getting available AI models.
 */
interface GetAIModelsUseCase {
    suspend operator fun invoke(): Result<List<AIModel>>
}

class GetAIModelsUseCaseImpl(
    private val aiRepository: AIRepository,
) : GetAIModelsUseCase {

    override suspend operator fun invoke(): Result<List<AIModel>> =
        aiRepository.getModels()
}
