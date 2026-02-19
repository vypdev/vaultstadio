/**
 * Use case for restoring a file to a previous version.
 */

package com.vaultstadio.app.domain.version.usecase

import com.vaultstadio.app.domain.result.Result

interface RestoreVersionUseCase {
    suspend operator fun invoke(itemId: String, versionNumber: Int, comment: String? = null): Result<Unit>
}
