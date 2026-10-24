/**
 * Use case for resolving a sync conflict.
 */

package com.vaultstadio.app.domain.sync.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.sync.model.ConflictResolution

interface ResolveConflictUseCase {
    suspend operator fun invoke(conflictId: String, resolution: ConflictResolution): Result<Unit>
}
