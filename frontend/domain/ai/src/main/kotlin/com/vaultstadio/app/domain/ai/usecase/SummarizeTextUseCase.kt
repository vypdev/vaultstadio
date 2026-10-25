/**
 * Use case for AI text summarization.
 */

package com.vaultstadio.app.domain.ai.usecase

import com.vaultstadio.app.domain.result.Result

interface SummarizeTextUseCase {
    suspend operator fun invoke(text: String, maxLength: Int = 200): Result<String>
}
