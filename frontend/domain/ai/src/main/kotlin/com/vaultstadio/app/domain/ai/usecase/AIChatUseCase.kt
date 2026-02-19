/**
 * Use case for AI chat interactions.
 */

package com.vaultstadio.app.domain.ai.usecase

import com.vaultstadio.app.domain.ai.model.AIChatMessage
import com.vaultstadio.app.domain.ai.model.AIChatResponse
import com.vaultstadio.app.domain.result.Result

interface AIChatUseCase {
    suspend operator fun invoke(
        messages: List<AIChatMessage>,
        model: String? = null,
        maxTokens: Int? = null,
        temperature: Double? = null,
    ): Result<AIChatResponse>
}
