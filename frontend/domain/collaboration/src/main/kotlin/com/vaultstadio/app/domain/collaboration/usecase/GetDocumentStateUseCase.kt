/**
 * Use case for getting document state.
 */

package com.vaultstadio.app.domain.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.model.DocumentState
import com.vaultstadio.app.domain.result.Result

interface GetDocumentStateUseCase {
    suspend operator fun invoke(itemId: String): Result<DocumentState>
}
