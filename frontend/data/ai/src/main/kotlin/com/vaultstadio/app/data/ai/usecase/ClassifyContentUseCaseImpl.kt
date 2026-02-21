/**
 * Classify Content Use Case implementation
 */

package com.vaultstadio.app.data.ai.usecase

import com.vaultstadio.app.domain.ai.AIRepository
import com.vaultstadio.app.domain.ai.usecase.ClassifyContentUseCase

class ClassifyContentUseCaseImpl(
    private val aiRepository: AIRepository,
) : ClassifyContentUseCase {
    override suspend operator fun invoke(content: String, categories: List<String>) =
        aiRepository.classify(content, categories)
}
