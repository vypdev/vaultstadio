/**
 * Batch Move Use Case
 */

package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.BatchResult

/**
 * Use case for batch moving items.
 */
interface BatchMoveUseCase {
    suspend operator fun invoke(itemIds: List<String>, destinationId: String?): Result<BatchResult>
}
