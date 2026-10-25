/**
 * AI Chat Use Case implementation
 */

package com.vaultstadio.app.data.ai.usecase

import com.vaultstadio.app.domain.ai.AIRepository
import com.vaultstadio.app.domain.ai.model.AIChatMessage
import com.vaultstadio.app.domain.ai.model.AIChatResponse
import com.vaultstadio.app.domain.ai.usecase.AIChatUseCase
import com.vaultstadio.app.domain.result.Result

class AIChatUseCaseImpl(
    private val aiRepository: AIRepository,
) : AIChatUseCase {
    override suspend operator fun invoke(
        messages: List<AIChatMessage>,
        model: String?,
        maxTokens: Int?,
        temperature: Double?,
    ): Result<AIChatResponse> =
        aiRepository.chat(messages, model, maxTokens, temperature)
}
