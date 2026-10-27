package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.core.domain.model.UserPresence
import com.vaultstadio.core.domain.repository.CollaborationRepository
import com.vaultstadio.domain.common.exception.StorageException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow

internal class CollaborationStreams(
    private val access: CollaborationAccess,
    private val presence: CollaborationPresence,
    private val repository: CollaborationRepository,
) {
    suspend fun operations(item: String, actor: String): Either<StorageException, Flow<CollaborationOperation>> =
        guarded({ access.item(item, actor).map { Unit } }, { repository.streamOperations(item) }) { Unit.right() }

    suspend fun comments(item: String, actor: String): Either<StorageException, Flow<DocumentComment>> =
        guarded({ access.item(item, actor).map { Unit } }, { repository.streamComments(item) }) {
            if (it.itemId != item) collaborationScopeFailure().left() else Unit.right()
        }

    suspend fun presence(users: List<String>, actor: String): Either<StorageException, Flow<UserPresence>> =
        guarded({
            presence.targets(users, actor)
        }, { if (users.isEmpty()) emptyFlow() else repository.streamPresence(users) }) {
            presence.bind(it, actor).map { Unit }
        }

    private suspend fun <T> guarded(
        authorize: suspend () -> Either<StorageException, Unit>,
        source: () -> Flow<T>,
        bind: suspend (T) -> Either<StorageException, Unit>,
    ): Either<StorageException, Flow<T>> = authorize().map {
        flow {
            authorize().collaborationValue()
            source().collect { value ->
                authorize().collaborationValue()
                bind(value).collaborationValue()
                emit(value)
            }
        }
    }
}
