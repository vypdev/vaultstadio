/**
 * Use case for resolving a document comment.
 */

package com.vaultstadio.app.domain.collaboration.usecase

import com.vaultstadio.app.domain.result.Result

interface ResolveDocumentCommentUseCase {
    suspend operator fun invoke(itemId: String, commentId: String): Result<Unit>
}
