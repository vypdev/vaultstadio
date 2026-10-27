package com.vaultstadio.api.routes.collaboration

import arrow.core.Either
import com.vaultstadio.core.domain.model.CommentReply
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.infrastructure.persistence.entities.DocumentCommentsTable
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CollaborationCommentWriteContractTest {
    private val f = CollaborationIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @BeforeEach fun reset() = f.reset()

    @AfterAll fun close() = f.close()

    @ParameterizedTest(name = "C89-COMMENT-WRITE {0}")
    @ValueSource(
        strings = [
            "create-forged-author", "reply-forged-author", "delete-other-author", "update-other-author",
            "update-forged-author", "update-own", "resolve-wrong-parent", "reply-wrong-parent",
            "delete-wrong-parent", "update-wrong-parent", "missing-comment", "reply-rollback",
        ],
    )
    fun `comment writes bind author parent and transaction result`(case: String) = testApplication {
        application { f.install(this) }
        startApplication()
        var comment = f.seedComment()
        if (case.endsWith("other-author")) {
            transaction {
                DocumentCommentsTable.update({
                    DocumentCommentsTable.id eq comment.id
                }) { it[userId] = f.storage.bob.id }
            }
            comment = comment.copy(userId = f.storage.bob.id)
        }
        val other = if (case.endsWith("wrong-parent")) f.storage.seed(type = ItemType.FILE) else null
        val before = f.snapshot()
        if (case == "reply-rollback") {
            transaction {
                exec("ALTER TABLE comment_replies ADD CONSTRAINT reject_test_reply CHECK (content <> 'reject')")
            }
        }
        val result = try {
            invoke(case, comment, other?.id ?: f.item.id)
        } finally {
            if (case ==
                "reply-rollback"
            ) {
                transaction { exec("ALTER TABLE comment_replies DROP CONSTRAINT reject_test_reply") }
            }
        }
        assertOutcome(case, comment, before, result)
    }

    private fun assertOutcome(case: String, comment: DocumentComment, before: List<Any>, result: Either<*, *>) {
        if (case == "update-own") {
            val updated = assertIs<DocumentComment>(assertIs<Either.Right<*>>(result).value)
            assertEquals("updated text", updated.content)
            assertEquals(comment.id, updated.id)
            assertEquals(comment.userId, updated.userId)
            assertEquals(before.last(), f.snapshot().last())
        } else {
            val error = assertIs<Either.Left<*>>(result).value
            when {
                case.endsWith("wrong-parent") || case == "missing-comment" -> assertIs<ItemNotFoundException>(error)
                case == "reply-rollback" -> assertIs<DatabaseException>(error)
                else -> assertIs<AuthorizationException>(error)
            }
            assertEquals(before, f.snapshot())
        }
    }

    private suspend fun invoke(case: String, comment: DocumentComment, item: String): Either<*, *> {
        val owner = f.storage.alice.id
        val reply = CommentReply(UUID.randomUUID().toString(), owner, "reject", f.item.createdAt)
        return when (case) {
            "create-forged-author" -> f.repository.createComment(
                comment.copy(id = UUID.randomUUID().toString(), userId = f.storage.bob.id),
                owner,
            )
            "reply-forged-author" -> f.repository.addReply(
                comment.id,
                reply.copy(userId = f.storage.bob.id),
                item,
                owner,
            )
            "delete-other-author", "delete-wrong-parent" -> f.repository.deleteComment(comment.id, item, owner)
            "resolve-wrong-parent" -> f.repository.resolveComment(comment.id, f.item.createdAt, item, owner)
            "reply-wrong-parent", "reply-rollback" -> f.repository.addReply(comment.id, reply, item, owner)
            "missing-comment" -> f.repository.resolveComment("absent", f.item.createdAt, item, owner)
            "update-forged-author" -> f.repository.updateComment(comment.copy(userId = f.storage.bob.id), owner)
            else -> f.repository.updateComment(comment.copy(content = "updated text", itemId = item), owner)
        }
    }
}
