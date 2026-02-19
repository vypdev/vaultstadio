/**
 * Use case for AI content classification.
 */

package com.vaultstadio.app.domain.ai.usecase

import com.vaultstadio.app.domain.result.Result

interface ClassifyContentUseCase {
    suspend operator fun invoke(content: String, categories: List<String>): Result<String>
}
