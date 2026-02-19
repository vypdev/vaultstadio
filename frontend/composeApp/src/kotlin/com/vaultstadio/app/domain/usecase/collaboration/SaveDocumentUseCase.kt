/**
 * Save Document Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.CollaborationRepository
import org.koin.core.annotation.Factory

/**
 * Use case for saving a collaborative document.
 */
interface SaveDocumentUseCase {
    suspend operator fun invoke(itemId: String): Result<Unit>
}

@Factory(binds = [SaveDocumentUseCase::class])
class SaveDocumentUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : SaveDocumentUseCase {

    override suspend operator fun invoke(itemId: String): Result<Unit> =
        collaborationRepository.saveDocument(itemId)
}
