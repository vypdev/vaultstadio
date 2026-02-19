/**
 * Use case for deleting an AI provider configuration.
 */

package com.vaultstadio.app.domain.ai.usecase

import com.vaultstadio.app.domain.ai.model.AIProviderType
import com.vaultstadio.app.domain.result.Result

interface DeleteAIProviderUseCase {
    suspend operator fun invoke(type: AIProviderType): Result<Unit>
}
