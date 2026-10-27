package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.CommentAnchor
import com.vaultstadio.core.domain.model.CommentReply
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.core.domain.repository.CollaborationRepository
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.StorageException
import kotlinx.datetime.Clock
import java.util.UUID

internal class CollaborationComments(
    private val access: CollaborationAccess,
    private val repository: CollaborationRepository,
) {
    suspend fun create(input: CreateCommentInput, actor: String): Either<StorageException, DocumentComment> =
        access.item(input.itemId, actor).flatMap {
            val now = Clock.System.now()
            val comment = DocumentComment(
                id = UUID.randomUUID().toString(),
                itemId = input.itemId,
                userId = actor,
                content = input.content,
                anchor = CommentAnchor(
                    input.startLine,
                    input.startColumn,
                    input.endLine,
                    input.endColumn,
                    input.quotedText,
                ),
                createdAt = now,
                updatedAt = now,
            )
            repository.createComment(comment).flatMap { row ->
                if (row.userId != actor) collaborationScopeFailure().left() else bind(input.itemId, comment.id, row)
            }
        }

    suspend fun list(item: String, actor: String, resolved: Boolean): Either<StorageException, List<DocumentComment>> =
        access.item(item, actor).flatMap {
            repository.getCommentsForItem(item, resolved).flatMap { rows ->
                if (rows.any { it.itemId != item } || rows.map { it.id }.toSet().size != rows.size) {
                    collaborationScopeFailure().left()
                } else {
                    rows.right()
                }
            }
        }

    suspend fun resolve(item: String, id: String, actor: String): Either<StorageException, DocumentComment> =
        access.comment(item, id, actor).flatMap {
            repository.resolveComment(id, Clock.System.now()).flatMap { row -> bind(item, id, row) }
        }

    suspend fun reply(
        item: String,
        id: String,
        content: String,
        actor: String,
    ): Either<StorageException, DocumentComment> =
        access.comment(item, id, actor).flatMap {
            val reply = CommentReply(
                id = UUID.randomUUID().toString(),
                userId = actor,
                content = content,
                createdAt = Clock.System.now(),
            )
            repository.addReply(id, reply).flatMap { row ->
                if (row.replies.singleOrNull { it.id == reply.id }?.let {
                        it.userId == actor && it.content == content
                    } != true
                ) {
                    collaborationScopeFailure().left()
                } else {
                    bind(item, id, row)
                }
            }
        }

    suspend fun delete(item: String, id: String, actor: String): Either<StorageException, Unit> =
        access.comment(item, id, actor).flatMap {
            if (it.userId != actor) AuthorizationException().left() else repository.deleteComment(id)
        }

    private fun bind(item: String, id: String, row: DocumentComment): Either<StorageException, DocumentComment> =
        if (row.itemId != item || row.id != id) collaborationScopeFailure().left() else row.right()
}
