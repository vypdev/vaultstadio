package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.CommentReply
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.core.domain.repository.CollaborationCommentsRepository
import com.vaultstadio.domain.common.exception.DatabaseException
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
    override suspend fun createComment(comment: DocumentComment): Either<StorageException, DocumentComment> {
        return collaborationRepositoryCall("Failed to create comment") {
            collaborationDbQuery {
                DocumentCommentsTable.insert {
                    it[id] = comment.id
                    it[itemId] = comment.itemId
                    it[userId] = comment.userId
                    it[content] = comment.content
                    it[anchorStartLine] = comment.anchor.startLine
                    it[anchorStartColumn] = comment.anchor.startColumn
                    it[anchorEndLine] = comment.anchor.endLine
                    it[anchorEndColumn] = comment.anchor.endColumn
                    it[quotedText] = comment.anchor.quotedText
                    it[createdAt] = comment.createdAt
                    it[updatedAt] = comment.updatedAt
                }
            }
            comment.right()
        }
    }

    override suspend fun findComment(commentId: String): Either<StorageException, DocumentComment?> {
        return collaborationRepositoryCall("Failed to find comment") {
            collaborationDbQuery {
                DocumentCommentsTable.selectAll()
                    .where { DocumentCommentsTable.id eq commentId }
                    .map { row ->
                        val replies = getRepliesForComment(commentId)
                        row.toDocumentComment(replies)
                    }
                    .singleOrNull()
            }.right()
        }
    }

    override suspend fun getCommentsForItem(
        itemId: String,
        includeResolved: Boolean,
    ): Either<StorageException, List<DocumentComment>> {
        return collaborationRepositoryCall("Failed to get comments") {
            collaborationDbQuery {
                val query = DocumentCommentsTable.selectAll()
                    .where { DocumentCommentsTable.itemId eq itemId }

                if (!includeResolved) {
                    query.andWhere { DocumentCommentsTable.resolvedAt.isNull() }
                }

                query.orderBy(DocumentCommentsTable.createdAt, SortOrder.ASC)
                    .map { row ->
                        val replies = getRepliesForComment(row[DocumentCommentsTable.id])
                        row.toDocumentComment(replies)
                    }
            }.right()
        }
    }

    override suspend fun updateComment(comment: DocumentComment): Either<StorageException, DocumentComment> {
        return collaborationRepositoryCall("Failed to update comment") {
            collaborationDbQuery {
                DocumentCommentsTable.update({ DocumentCommentsTable.id eq comment.id }) {
                    it[content] = comment.content
                    it[updatedAt] = Clock.System.now()
                }
            }
            comment.right()
        }
    }

    override suspend fun resolveComment(
        commentId: String,
        resolvedAt: Instant,
    ): Either<StorageException, DocumentComment> {
        return collaborationRepositoryCall("Failed to resolve comment") {
            collaborationDbQuery {
                DocumentCommentsTable.update({ DocumentCommentsTable.id eq commentId }) {
                    it[DocumentCommentsTable.resolvedAt] = resolvedAt
                    it[updatedAt] = Clock.System.now()
                }
            }
            findComment(commentId).fold(
                { it.left() },
                { comment -> comment?.right() ?: DatabaseException("Comment not found").left() },
            )
        }
    }

    override suspend fun addReply(
        commentId: String,
        reply: CommentReply,
    ): Either<StorageException, DocumentComment> {
        return collaborationRepositoryCall("Failed to add reply") {
            collaborationDbQuery {
                CommentRepliesTable.insert {
                    it[id] = reply.id
                    it[CommentRepliesTable.commentId] = commentId
                    it[userId] = reply.userId
                    it[content] = reply.content
                    it[createdAt] = reply.createdAt
                }
            }
            findComment(commentId).fold(
                { it.left() },
                { comment -> comment?.right() ?: DatabaseException("Comment not found").left() },
            )
        }
    }

    override suspend fun deleteComment(commentId: String): Either<StorageException, Unit> {
        return collaborationRepositoryCall("Failed to delete comment") {
            collaborationDbQuery {
                CommentRepliesTable.deleteWhere { CommentRepliesTable.commentId eq commentId }
                DocumentCommentsTable.deleteWhere { DocumentCommentsTable.id eq commentId }
            }
            Unit.right()
        }
    }

    override fun streamComments(itemId: String): Flow<DocumentComment> = flow {
        // Real-time streaming would use a different mechanism
    }

    private fun getRepliesForComment(commentId: String): List<CommentReply> {
        return CommentRepliesTable.selectAll()
            .where { CommentRepliesTable.commentId eq commentId }
            .orderBy(CommentRepliesTable.createdAt, SortOrder.ASC)
            .map { it.toCommentReply() }
    }
}
