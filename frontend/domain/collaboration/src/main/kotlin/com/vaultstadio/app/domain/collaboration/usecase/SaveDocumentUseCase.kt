/**
 * Use case for saving document.
 */

package com.vaultstadio.app.domain.collaboration.usecase

import com.vaultstadio.app.domain.result.Result

interface SaveDocumentUseCase {
    suspend operator fun invoke(itemId: String): Result<Unit>
}
