/**
 * Use case for configuring an AI provider.
 */

package com.vaultstadio.app.domain.ai.usecase

import com.vaultstadio.app.domain.ai.model.AIProviderType
import com.vaultstadio.app.domain.result.Result

interface ConfigureAIProviderUseCase {
    suspend operator fun invoke(
        type: AIProviderType,
        baseUrl: String,
        apiKey: String? = null,
        model: String? = null,
    ): Result<String>
}
