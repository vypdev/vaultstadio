/**
 * Use case for comparing two versions of a file.
 */

package com.vaultstadio.app.domain.version.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.version.model.VersionDiff

interface CompareVersionsUseCase {
    suspend operator fun invoke(itemId: String, fromVersion: Int, toVersion: Int): Result<VersionDiff>
}
