/**
 * Use case for setting the active AI provider.
 */

package com.vaultstadio.app.domain.ai.usecase

import com.vaultstadio.app.domain.ai.model.AIProviderType
import com.vaultstadio.app.domain.result.Result

interface SetActiveAIProviderUseCase {
    suspend operator fun invoke(type: AIProviderType): Result<String>
}
