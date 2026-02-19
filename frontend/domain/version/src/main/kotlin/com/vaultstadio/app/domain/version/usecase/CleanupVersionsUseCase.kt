/**
 * Use case for cleaning up old versions of a file.
 */

package com.vaultstadio.app.domain.version.usecase

import com.vaultstadio.app.domain.result.Result

interface CleanupVersionsUseCase {
    suspend operator fun invoke(
        itemId: String,
        maxVersions: Int? = null,
        maxAgeDays: Int? = null,
        minVersionsToKeep: Int = 1,
    ): Result<Unit>
}
