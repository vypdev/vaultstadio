/**
 * Classify Content Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AIRepository
import org.koin.core.annotation.Factory

/**
 * Use case for AI content classification.
 */
interface ClassifyContentUseCase {
    suspend operator fun invoke(content: String, categories: List<String>): ApiResult<String>
}

@Factory(binds = [ClassifyContentUseCase::class])
class ClassifyContentUseCaseImpl(
    private val aiRepository: AIRepository,
) : ClassifyContentUseCase {

    override suspend operator fun invoke(content: String, categories: List<String>): ApiResult<String> =
        aiRepository.classify(content, categories)
}
