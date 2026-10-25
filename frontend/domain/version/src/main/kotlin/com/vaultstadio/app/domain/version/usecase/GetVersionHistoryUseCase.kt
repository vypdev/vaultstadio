/**
 * Use case for getting the version history of a file.
 */

package com.vaultstadio.app.domain.version.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.version.model.FileVersionHistory

interface GetVersionHistoryUseCase {
    suspend operator fun invoke(itemId: String): Result<FileVersionHistory>
}
