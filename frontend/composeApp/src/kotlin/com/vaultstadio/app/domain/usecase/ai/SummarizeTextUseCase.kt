/**
 * Summarize Text Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.AIRepository
/**
 * Use case for AI text summarization.
 */
interface SummarizeTextUseCase {
    suspend operator fun invoke(text: String, maxLength: Int = 200): Result<String>
}

class SummarizeTextUseCaseImpl(
    private val aiRepository: AIRepository,
) : SummarizeTextUseCase {

    override suspend operator fun invoke(text: String, maxLength: Int): Result<String> =
        aiRepository.summarize(text, maxLength)
}
