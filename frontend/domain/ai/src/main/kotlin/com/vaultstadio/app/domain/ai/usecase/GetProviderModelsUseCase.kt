/**
 * Use case for getting models available for a specific AI provider.
 */

package com.vaultstadio.app.domain.ai.usecase

import com.vaultstadio.app.domain.ai.model.AIModel
import com.vaultstadio.app.domain.ai.model.AIProviderType
import com.vaultstadio.app.domain.result.Result

interface GetProviderModelsUseCase {
    suspend operator fun invoke(type: AIProviderType): Result<List<AIModel>>
}
