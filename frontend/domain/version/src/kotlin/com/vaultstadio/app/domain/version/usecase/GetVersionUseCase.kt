/**
 * Use case for getting a specific version of a file.
 */

package com.vaultstadio.app.domain.version.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.version.model.FileVersion

interface GetVersionUseCase {
    suspend operator fun invoke(itemId: String, versionNumber: Int): Result<FileVersion>
}
