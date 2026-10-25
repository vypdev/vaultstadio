/**
 * Use case for getting available AI models.
 */

package com.vaultstadio.app.domain.ai.usecase

import com.vaultstadio.app.domain.ai.model.AIModel
import com.vaultstadio.app.domain.result.Result

interface GetAIModelsUseCase {
    suspend operator fun invoke(): Result<List<AIModel>>
}
