/**
 * Use case for creating a comment on a document.
 */

package com.vaultstadio.app.domain.collaboration.usecase

import com.vaultstadio.app.domain.result.Result

interface CreateDocumentCommentUseCase {
    suspend operator fun invoke(
        itemId: String,
        content: String,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
    ): Result<Unit>
}
