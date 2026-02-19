/**
 * Use case for getting comments on a document.
 */

package com.vaultstadio.app.domain.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.model.DocumentComment
import com.vaultstadio.app.domain.result.Result

interface GetDocumentCommentsUseCase {
    suspend operator fun invoke(itemId: String, includeResolved: Boolean = false): Result<List<DocumentComment>>
}
