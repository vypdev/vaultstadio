package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.CommentReply
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertIs

class CollaborationReturnedStateTest {
    @ParameterizedTest(name = "C89-RETURN {0}")
    @ValueSource(
        strings = [
            "apply-item",
            "save-item",
            "cursor-id",
            "cursor-user",
            "created-user",
            "created-item",
            "created-id",
            "comment-list-parent",
            "comment-list-duplicate",
            "resolved-item",
            "resolved-id",
            "reply-duplicate",
            "reply-missing",
            "reply-user",
            "reply-content",
            "reply-parent",
            "participant-expected-item",
            "cleanup-blank",
            "cleanup-negative",
            "cleanup-positive",
            "missing-full-session",
            "missing-full-comment",
            "participant-scope-failure",
            "blank-session-actor",
        ],
    )
    fun `returned state and maintenance outcomes cannot bypass scope binding`(case: String) = runTest {
        val f = CollaborationServiceFixture()
        arrange(f, case)
        val result = invoke(f, case)
        if (case == "participant-scope-failure") {
            // getSession binds each returned participant through the minimal participant-session projection.
            coVerify(exactly = 1) { f.scopes.participantSession(f.participant.id) }
        }
        when (case) {
            "cleanup-positive" -> assertIs<Either.Right<*>>(result)
            "cleanup-blank", "blank-session-actor", "participant-expected-item" ->
                assertIs<AuthorizationException>(assertIs<Either.Left<*>>(result).value)
            "missing-full-session", "missing-full-comment" ->
                assertIs<ItemNotFoundException>(assertIs<Either.Left<*>>(result).value)
            else -> assertIs<DatabaseException>(assertIs<Either.Left<*>>(result).value)
        }
    }

    private fun arrange(f: CollaborationServiceFixture, case: String) {
        when (case) {
            "apply-item" -> coEvery { f.repository.applyOperation(any(), any(), any(), any()) } returns
                f.state.copy(itemId = "foreign").right()
            "save-item" -> coEvery { f.repository.saveExistingSnapshot(any(), any()) } returns
                f.state.copy(itemId = "foreign").right()
            "cursor-id", "cursor-user" -> coEvery { f.membership.update(any(), any(), any(), any(), any()) } answers {
                val row = arg<CollaborationParticipant>(2)
                if (case == "cursor-id") row.copy(id = "foreign").right() else row.copy(userId = "foreign").right()
            }
            "cleanup-negative" -> coEvery { f.membership.closeExpiredForOwner(any(), any()) } returns (-1).right()
            "cleanup-positive" -> coEvery { f.membership.closeExpiredForOwner(any(), any()) } returns 1.right()
            "missing-full-session" -> coEvery { f.repository.findSession(any()) } returns null.right()
            "missing-full-comment" -> coEvery { f.repository.findComment(any()) } returns null.right()
            "participant-scope-failure" -> coEvery { f.scopes.participantSession(any()) } returns
                DatabaseException("private scope failure").left()
            else -> arrangeComments(f, case)
        }
    }

    private fun arrangeComments(f: CollaborationServiceFixture, case: String) {
        when (case) {
            "created-user", "created-item", "created-id" -> coEvery { f.repository.createComment(any(), any()) } answers
                {
                    val row = firstArg<DocumentComment>()
                    when (case) {
                        "created-user" -> row.copy(userId = "foreign")
                        "created-item" -> row.copy(itemId = "foreign")
                        else -> row.copy(id = "foreign")
                    }.right()
                }
            "comment-list-parent" -> coEvery { f.repository.getCommentsForItem(any(), any()) } returns
                listOf(f.comment.copy(itemId = "foreign")).right()
            "comment-list-duplicate" -> coEvery { f.repository.getCommentsForItem(any(), any()) } returns
                listOf(f.comment, f.comment).right()
            "resolved-item" -> coEvery { f.repository.resolveComment(any(), any(), any(), any()) } returns
                f.comment.copy(itemId = "foreign").right()
            "resolved-id" -> coEvery { f.repository.resolveComment(any(), any(), any(), any()) } returns
                f.comment.copy(id = "foreign").right()
            "reply-duplicate", "reply-missing", "reply-user", "reply-content", "reply-parent" -> arrangeReply(f, case)
        }
    }

    private fun arrangeReply(f: CollaborationServiceFixture, case: String) {
        coEvery { f.repository.addReply(any(), any(), any(), any()) } answers {
            val reply = secondArg<CommentReply>()
            val returned = when (case) {
                "reply-duplicate" -> listOf(reply, reply)
                "reply-missing" -> emptyList()
                "reply-user" -> listOf(reply.copy(userId = "foreign"))
                "reply-content" -> listOf(reply.copy(content = "other content"))
                else -> listOf(reply)
            }
            f.comment.copy(itemId = if (case == "reply-parent") "foreign" else f.item.id, replies = returned).right()
        }
    }

    private suspend fun invoke(f: CollaborationServiceFixture, case: String): Either<*, *> = when (case) {
        "apply-item" -> f.service.applyOperation(f.item.id, f.operation, "user-1")
        "save-item" -> f.service.saveDocument(f.item.id, "user-1")
        "cursor-id", "cursor-user" -> f.service.updateSelection(f.session.id, f.participant.id, null, "user-1")
        "created-user", "created-item", "created-id" -> f.service.createComment(
            CreateCommentInput(f.item.id, "new", 0, 0, 1, 1),
            "user-1",
        )
        "comment-list-parent", "comment-list-duplicate" -> f.service.getComments(f.item.id, "user-1")
        "resolved-item", "resolved-id", "missing-full-comment" -> f.service.resolveComment(
            f.item.id,
            f.comment.id,
            "user-1",
        )
        "reply-duplicate", "reply-missing", "reply-user", "reply-content", "reply-parent" -> f.service.addReply(
            f.item.id,
            f.comment.id,
            "reply",
            "user-1",
        )
        "participant-expected-item" -> f.service.authorizeParticipant(
            f.session.id,
            f.participant.id,
            "user-1",
            "unbound-item",
        )
        "cleanup-blank" -> f.service.cleanupExpiredSessions(" ")
        "cleanup-negative", "cleanup-positive" -> f.service.cleanupExpiredSessions("user-1")
        "blank-session-actor" -> f.service.getSession(f.session.id, " ")
        else -> f.service.getSession(f.session.id, "user-1")
    }
}
