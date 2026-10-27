package com.vaultstadio.core.domain.service

import arrow.core.Either
import com.vaultstadio.core.domain.model.CursorPosition
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.storage.model.Visibility
import io.mockk.Called
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertIs

class CollaborationOwnershipMatrixTest {
    @ParameterizedTest(name = "C89-SVC {0} {1} {2}")
    @MethodSource("cases")
    fun `every scoped application entrypoint requires canonical ownership`(
        op: String,
        visibility: Visibility,
        actor: String,
    ) = runTest {
        val f = CollaborationServiceFixture()
        f.item = f.item.copy(visibility = visibility)
        val result = invoke(f, op, actor)
        if (actor == "user-1") {
            assertIs<Either.Right<*>>(result)
        } else {
            assertIs<AuthorizationException>(assertIs<Either.Left<*>>(result).value)
            verify {
                f.repository wasNot Called
                f.membership wasNot Called
            }
        }
    }

    private suspend fun invoke(f: CollaborationServiceFixture, op: String, actor: String): Either<*, *> {
        val s = f.service
        return when (op) {
            "join" -> s.joinSession(JoinSessionInput(f.item.id), actor, "display")
            "leave" -> s.leaveSession(f.session.id, "p1", actor)
            "session" -> s.getSession(f.session.id, actor)
            "participants" -> s.getParticipants(f.session.id, actor)
            "authorizeParticipant" -> s.authorizeParticipant(f.session.id, "p1", actor, f.item.id)
            "cursor" -> s.updateCursor(f.session.id, "p1", CursorPosition(2, 3, 4), actor)
            "selection" -> s.updateSelection(f.session.id, "p1", null, actor)
            "document" -> s.getDocumentState(f.item.id, actor)
            "apply" -> s.applyOperation(f.item.id, f.operation, actor)
            "operations" -> s.getOperationsSince(f.item.id, 0, actor)
            "save" -> s.saveDocument(f.item.id, actor)
            "streamOperations" -> s.streamOperations(f.item.id, actor)
            "createComment" -> s.createComment(CreateCommentInput(f.item.id, "comment", 0, 0, 1, 1), actor)
            "comments" -> s.getComments(f.item.id, actor)
            "resolve" -> s.resolveComment(f.item.id, f.comment.id, actor)
            "reply" -> s.addReply(f.item.id, f.comment.id, "reply", actor)
            "delete" -> s.deleteComment(f.item.id, f.comment.id, actor)
            "streamComments" -> s.streamComments(f.item.id, actor)
            else -> error("Unenumerated operation")
        }
    }

    companion object {
        @JvmStatic
        fun cases(): List<Arguments> = listOf(
            "join",
            "leave",
            "session",
            "participants",
            "authorizeParticipant",
            "cursor",
            "selection",
            "document",
            "apply",
            "operations",
            "save",
            "streamOperations",
            "createComment",
            "comments",
            "resolve",
            "reply",
            "delete",
            "streamComments",
        ).flatMap { op ->
            Visibility.entries.flatMap { visibility ->
                listOf(
                    "user-1",
                    "unrelated",
                ).map { Arguments.of(op, visibility, it) }
            }
        }
    }
}
