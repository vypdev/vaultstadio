/**
 * AI Chat Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AIRepository
import com.vaultstadio.app.domain.model.AIChatMessage
import com.vaultstadio.app.domain.model.AIChatResponse
import org.koin.core.annotation.Factory

/**
 * Use case for AI chat interactions.
 */
interface AIChatUseCase {
    suspend operator fun invoke(
        messages: List<AIChatMessage>,
        model: String? = null,
        maxTokens: Int? = null,
        temperature: Double? = null,
    ): ApiResult<AIChatResponse>
}

@Factory(binds = [AIChatUseCase::class])
class AIChatUseCaseImpl(
    private val aiRepository: AIRepository,
) : AIChatUseCase {

    override suspend operator fun invoke(
        messages: List<AIChatMessage>,
        model: String?,
        maxTokens: Int?,
        temperature: Double?,
    ): ApiResult<AIChatResponse> = aiRepository.chat(messages, model, maxTokens, temperature)
}
