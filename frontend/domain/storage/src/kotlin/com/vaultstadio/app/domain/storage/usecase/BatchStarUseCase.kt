/**
 * Batch Star Use Case
 */

package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.BatchResult

/**
 * Use case for batch starring/unstarring items.
 */
interface BatchStarUseCase {
    suspend operator fun invoke(itemIds: List<String>, starred: Boolean): Result<BatchResult>
}
