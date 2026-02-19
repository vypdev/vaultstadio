/**
 * Use case for deleting a specific version of a file.
 */

package com.vaultstadio.app.domain.version.usecase

import com.vaultstadio.app.domain.result.Result

interface DeleteVersionUseCase {
    suspend operator fun invoke(versionId: String): Result<Unit>
}
