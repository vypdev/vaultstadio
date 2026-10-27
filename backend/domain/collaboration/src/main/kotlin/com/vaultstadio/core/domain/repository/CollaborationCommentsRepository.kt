package com.vaultstadio.core.domain.repository

import arrow.core.Either
import com.vaultstadio.core.domain.model.CommentReply
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.domain.common.exception.StorageException
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface CollaborationCommentsRepository {
    /**
     * Create a comment.
     *
     * @param comment The comment to create
     * @return The created comment
     */
    suspend fun createComment(comment: DocumentComment): Either<StorageException, DocumentComment>

    /**
     * Find a comment by ID.
     *
     * @param commentId Comment ID
     * @return The comment or null
     */
    suspend fun findComment(commentId: String): Either<StorageException, DocumentComment?>

    /**
     * Get comments for an item.
     *
     * @param itemId Storage item ID
     * @param includeResolved Include resolved comments
     * @return List of comments
     */
    suspend fun getCommentsForItem(
        itemId: String,
        includeResolved: Boolean = false,
    ): Either<StorageException, List<DocumentComment>>

    /**
     * Update a comment.
     *
     * @param comment Updated comment
     * @return The updated comment
     */
    suspend fun updateComment(comment: DocumentComment): Either<StorageException, DocumentComment>

    /**
     * Resolve a comment.
     *
     * @param commentId Comment ID
     * @param resolvedAt When it was resolved
     * @return The updated comment
     */
    suspend fun resolveComment(
        commentId: String,
        resolvedAt: Instant,
    ): Either<StorageException, DocumentComment>

    /**
     * Add a reply to a comment.
     *
     * @param commentId Comment ID
     * @param reply The reply to add
     * @return The updated comment
     */
    suspend fun addReply(
        commentId: String,
        reply: CommentReply,
    ): Either<StorageException, DocumentComment>

    /**
     * Delete a comment.
     *
     * @param commentId Comment ID
     * @return Unit on success
     */
    suspend fun deleteComment(commentId: String): Either<StorageException, Unit>

    /**
     * Stream comments for an item.
     *
     * @param itemId Storage item ID
     * @return Flow of comment updates
     */
    fun streamComments(itemId: String): Flow<DocumentComment>
}
