package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.core.domain.model.CommentReply
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.infrastructure.persistence.entities.CommentRepliesTable
import com.vaultstadio.infrastructure.persistence.entities.DocumentCommentsTable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

internal fun insertCollaborationComment(comment: DocumentComment) {
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

internal fun readCollaborationReplies(comment: String): List<CommentReply> = CommentRepliesTable.selectAll()
    .where { CommentRepliesTable.commentId eq comment }.orderBy(CommentRepliesTable.createdAt, SortOrder.ASC)
    .map { it.toCommentReply() }

internal fun requireCollaborationComment(item: String, id: String): DocumentComment = DocumentCommentsTable.selectAll()
    .where { DocumentCommentsTable.id eq id and (DocumentCommentsTable.itemId eq item) }.forUpdate().singleOrNull()
    ?.toDocumentComment(readCollaborationReplies(id)) ?: throw ItemNotFoundException(message = "Comment not found")
