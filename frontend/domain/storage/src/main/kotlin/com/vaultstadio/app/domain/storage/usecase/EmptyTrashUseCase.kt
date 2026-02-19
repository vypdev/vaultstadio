/**
 * Empty Trash Use Case
 */

package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.BatchResult

/**
 * Use case for emptying trash.
 */
interface EmptyTrashUseCase {
    suspend operator fun invoke(): Result<BatchResult>
}
