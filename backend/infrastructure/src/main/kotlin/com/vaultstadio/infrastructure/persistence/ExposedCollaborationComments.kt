package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.core.domain.model.CommentReply
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.core.domain.repository.CollaborationCommentsRepository
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.infrastructure.persistence.entities.CommentRepliesTable
import com.vaultstadio.infrastructure.persistence.entities.DocumentCommentsTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

internal class ExposedCollaborationComments : CollaborationCommentsRepository {
    override suspend fun createComment(
        comment: DocumentComment,
        actorId: String,
    ): Either<StorageException, DocumentComment> =
        write(comment.itemId, actorId, "Failed to create comment") {
            if (comment.userId != actorId) throw AuthorizationException()
            insertCollaborationComment(comment)
            requireCollaborationComment(comment.itemId, comment.id)
        }

    override suspend fun findComment(commentId: String): Either<StorageException, DocumentComment?> =
        collaborationRepositoryCall("Failed to find comment") {
            collaborationDbQuery {
                DocumentCommentsTable.selectAll().where { DocumentCommentsTable.id eq commentId }
                    .singleOrNull()?.toDocumentComment(readCollaborationReplies(commentId))
            }.right()
        }

    override suspend fun getCommentsForItem(
        itemId: String,
        includeResolved: Boolean,
    ): Either<StorageException, List<DocumentComment>> = collaborationRepositoryCall("Failed to get comments") {
        collaborationDbQuery {
            val query = DocumentCommentsTable.selectAll().where { DocumentCommentsTable.itemId eq itemId }
            if (!includeResolved) query.andWhere { DocumentCommentsTable.resolvedAt.isNull() }
            query.orderBy(DocumentCommentsTable.createdAt, SortOrder.ASC).map { row ->
                row.toDocumentComment(readCollaborationReplies(row[DocumentCommentsTable.id]))
            }
        }.right()
    }

    override suspend fun updateComment(
        comment: DocumentComment,
        actorId: String,
    ): Either<StorageException, DocumentComment> =
        write(comment.itemId, actorId, "Failed to update comment") {
            val existing = requireCollaborationComment(comment.itemId, comment.id)
            if (existing.userId != actorId || comment.userId != actorId) throw AuthorizationException()
            DocumentCommentsTable.update({ DocumentCommentsTable.id eq comment.id }) {
                it[content] = comment.content
                it[updatedAt] = Clock.System.now()
            }
            requireCollaborationComment(comment.itemId, comment.id)
        }

    override suspend fun resolveComment(
        commentId: String,
        resolvedAt: Instant,
        itemId: String,
        actorId: String,
    ): Either<StorageException, DocumentComment> = write(itemId, actorId, "Failed to resolve comment") {
        requireCollaborationComment(itemId, commentId)
        DocumentCommentsTable.update({ DocumentCommentsTable.id eq commentId }) {
            it[DocumentCommentsTable.resolvedAt] = resolvedAt
            it[updatedAt] = Clock.System.now()
        }
        requireCollaborationComment(itemId, commentId)
    }

    override suspend fun addReply(
        commentId: String,
        reply: CommentReply,
        itemId: String,
        actorId: String,
    ): Either<StorageException, DocumentComment> = write(itemId, actorId, "Failed to add reply") {
        requireCollaborationComment(itemId, commentId)
        if (reply.userId != actorId) throw AuthorizationException()
        CommentRepliesTable.insert {
            it[id] = reply.id
            it[CommentRepliesTable.commentId] = commentId
            it[userId] = reply.userId
            it[content] = reply.content
            it[createdAt] = reply.createdAt
        }
        requireCollaborationComment(itemId, commentId)
    }

    override suspend fun deleteComment(
        commentId: String,
        itemId: String,
        actorId: String,
    ): Either<StorageException, Unit> =
        write(itemId, actorId, "Failed to delete comment") {
            if (requireCollaborationComment(itemId, commentId).userId != actorId) throw AuthorizationException()
            CommentRepliesTable.deleteWhere { CommentRepliesTable.commentId eq commentId }
            DocumentCommentsTable.deleteWhere { DocumentCommentsTable.id eq commentId }
            Unit
        }

    override fun streamComments(itemId: String): Flow<DocumentComment> = flow {
        // Durable delivery remains the separate R14 capability.
    }

    private suspend fun <T> write(
        item: String,
        actor: String,
        description: String,
        effect: () -> T,
    ): Either<StorageException, T> = collaborationRepositoryCall(description) {
        collaborationDbQuery {
            lockCollaborationItem(item, actor)
            effect()
        }.right()
    }
}
