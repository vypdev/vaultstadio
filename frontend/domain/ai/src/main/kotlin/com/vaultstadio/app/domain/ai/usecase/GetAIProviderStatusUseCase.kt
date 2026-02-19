/**
 * Use case for checking the status of an AI provider.
 */

package com.vaultstadio.app.domain.ai.usecase

import com.vaultstadio.app.domain.ai.model.AIProviderType
import com.vaultstadio.app.domain.result.Result

interface GetAIProviderStatusUseCase {
    suspend operator fun invoke(type: AIProviderType): Result<Map<String, Boolean>>
}
