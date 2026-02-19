/**
 * Batch Delete Use Case
 */

package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.BatchResult

/**
 * Use case for batch deleting items.
 */
interface BatchDeleteUseCase {
    suspend operator fun invoke(itemIds: List<String>, permanent: Boolean = false): Result<BatchResult>
}
