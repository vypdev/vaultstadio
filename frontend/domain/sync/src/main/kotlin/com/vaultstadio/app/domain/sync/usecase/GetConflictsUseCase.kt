/**
 * Use case for getting sync conflicts.
 */

package com.vaultstadio.app.domain.sync.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.sync.model.SyncConflict

interface GetConflictsUseCase {
    suspend operator fun invoke(): Result<List<SyncConflict>>
}
