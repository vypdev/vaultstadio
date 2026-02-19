/**
 * Use case for getting available AI providers.
 */

package com.vaultstadio.app.domain.ai.usecase

import com.vaultstadio.app.domain.ai.model.AIProviderInfo
import com.vaultstadio.app.domain.result.Result

interface GetAIProvidersUseCase {
    suspend operator fun invoke(): Result<List<AIProviderInfo>>
}
