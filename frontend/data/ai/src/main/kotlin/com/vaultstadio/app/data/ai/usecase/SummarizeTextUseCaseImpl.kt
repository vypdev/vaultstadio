/**
 * Summarize Text Use Case implementation
 */

package com.vaultstadio.app.data.ai.usecase

import com.vaultstadio.app.domain.ai.AIRepository
import com.vaultstadio.app.domain.ai.usecase.SummarizeTextUseCase

class SummarizeTextUseCaseImpl(
    private val aiRepository: AIRepository,
) : SummarizeTextUseCase {
    override suspend operator fun invoke(text: String, maxLength: Int) =
        aiRepository.summarize(text, maxLength)
}
