package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.collaboration.repository.CollaborationSessionScope
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.InvalidOperationException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.storage.model.ItemType
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class CollaborationFaultContractTest {
    @ParameterizedTest(name = "C89-FAULT {0}")
    @ValueSource(
        strings = [
            "missing-item",
            "trashed-item",
            "folder-item",
            "wrong-item",
            "blank-actor",
            "item-provider",
            "missing-session",
            "scope-provider",
            "wrong-session",
            "wrong-session-item",
            "closed-session",
            "expired-session",
            "duplicate-participants",
            "wrong-participant-scope",
            "foreign-participant",
            "missing-comment",
            "wrong-comment-parent",
            "wrong-comment-row",
            "wrong-comment-item",
            "wrong-document",
            "forged-operation-actor",
            "missing-save",
            "typed-save-failure",
            "stream-revocation",
        ],
    )
    fun `scope and fault contracts fail closed`(case: String) = runTest {
        val f = CollaborationServiceFixture()
        val expected = arrange(f, case)
        if (case == "stream-revocation") {
            val subscription = f.service.streamOperations(f.item.id, "user-1").socketFreeValue()
            f.item = f.item.copy(ownerId = "new-owner")
            assertFailsWith<AuthorizationException> { subscription.toList() }
            verify(exactly = 0) { f.repository.streamOperations(any()) }
        } else {
            val result = invoke(f, case)
            val error = assertIs<Either.Left<*>>(result).value
            assertEquals(expected, error!!::class)
            if (case in
                setOf("missing-item", "trashed-item", "folder-item", "wrong-item", "blank-actor", "item-provider")
            ) {
                verify {
                    f.repository wasNot Called
                    f.membership wasNot Called
                }
            }
            if (case == "wrong-comment-parent") coVerify(exactly = 0) { f.repository.findComment(any()) }
        }
    }

    private fun arrange(f: CollaborationServiceFixture, case: String): KClass<*> = when (case) {
        "missing-item" -> {
            coEvery { f.items.findById(any()) } returns null.right()
            ItemNotFoundException::class
        }
        "trashed-item" -> {
            f.item = f.item.copy(isTrashed = true)
            InvalidOperationException::class
        }
        "folder-item" -> {
            f.item = f.item.copy(type = ItemType.FOLDER)
            InvalidOperationException::class
        }
        "wrong-item" -> {
            f.item = f.item.copy(id = "unrequested")
            DatabaseException::class
        }
        "blank-actor" -> {
            AuthorizationException::class
        }
        "item-provider" -> {
            coEvery {
                f.items.findById(any())
            } returns DatabaseException("private provider").left()
            DatabaseException::class
        }
        else -> {
            arrangeSession(f, case)
        }
    }

    private fun arrangeSession(f: CollaborationServiceFixture, case: String): KClass<*> = when (case) {
        "missing-session" -> {
            coEvery { f.scopes.session(any()) } returns null.right()
            ItemNotFoundException::class
        }
        "scope-provider" -> {
            coEvery {
                f.scopes.session(any())
            } returns DatabaseException("scope failure").left()
            DatabaseException::class
        }
        "wrong-session" -> {
            f.session = f.session.copy(id = "other-session")
            DatabaseException::class
        }
        "wrong-session-item" -> {
            f.session = f.session.copy(itemId = "other-item")
            DatabaseException::class
        }
        "closed-session" -> {
            coEvery {
                f.scopes.session(any())
            } returns CollaborationSessionScope(f.item.id, true).right()
            InvalidOperationException::class
        }
        "expired-session" -> {
            f.session = f.session.copy(expiresAt = f.now)
            InvalidOperationException::class
        }
        "duplicate-participants" -> {
            f.session = f.session.copy(participants = listOf(f.participant, f.participant))
            DatabaseException::class
        }
        "wrong-participant-scope" -> {
            coEvery {
                f.scopes.participantSession(any())
            } returns "foreign-session".right()
            DatabaseException::class
        }
        "foreign-participant" -> {
            f.session = f.session.copy(participants = listOf(f.participant.copy(userId = "other")))
            AuthorizationException::class
        }
        else -> {
            arrangeComment(f, case)
        }
    }

    private fun arrangeComment(f: CollaborationServiceFixture, case: String): KClass<*> = when (case) {
        "missing-comment" -> {
            coEvery {
                f.scopes.commentItem(any())
            } returns null.right()
            ItemNotFoundException::class
        }
        "wrong-comment-parent" -> {
            coEvery {
                f.scopes.commentItem(any())
            } returns "other-item".right()
            ItemNotFoundException::class
        }
        "wrong-comment-row" -> {
            coEvery {
                f.repository.findComment(any())
            } returns f.comment.copy(id = "other-comment").right()
            DatabaseException::class
        }
        "wrong-comment-item" -> {
            coEvery {
                f.repository.findComment(any())
            } returns f.comment.copy(itemId = "other-item").right()
            DatabaseException::class
        }
        else -> {
            arrangeDocument(f, case)
        }
    }

    private fun arrangeDocument(f: CollaborationServiceFixture, case: String): KClass<*> = when (case) {
        "wrong-document" -> {
            coEvery {
                f.repository.getDocumentState(any())
            } returns f.state.copy(itemId = "other-item").right()
            DatabaseException::class
        }
        "forged-operation-actor" -> {
            AuthorizationException::class
        }
        "missing-save" -> {
            coEvery {
                f.repository.getDocumentState(any())
            } returns null.right()
            ItemNotFoundException::class
        }
        "typed-save-failure" -> {
            coEvery {
                f.repository.saveDocumentState(any())
            } returns DatabaseException("provider key").left()
            DatabaseException::class
        }
        "stream-revocation" -> {
            AuthorizationException::class
        }
        else -> {
            error("Unenumerated fault")
        }
    }

    private suspend fun invoke(f: CollaborationServiceFixture, case: String): Either<*, *> = when (case) {
        "blank-actor" -> f.service.getDocumentState("item-1", " ")
        "missing-item",
        "trashed-item",
        "folder-item",
        "wrong-item",
        "item-provider",
        "wrong-document",
        -> f.service.getDocumentState(
            "item-1",
            "user-1",
        )
        "missing-comment",
        "wrong-comment-parent",
        "wrong-comment-row",
        "wrong-comment-item",
        -> f.service.resolveComment(
            "item-1",
            "comment-1",
            "user-1",
        )
        "foreign-participant" -> f.service.authorizeParticipant("session-1", "p1", "user-1", "item-1")
        "forged-operation-actor" -> f.service.applyOperation("item-1", f.operation.copy(userId = "forged"), "user-1")
        "missing-save", "typed-save-failure" -> f.service.saveDocument("item-1", "user-1")
        else -> f.service.getSession("session-1", "user-1")
    }
}

private fun <A, B> Either<A, B>.socketFreeValue(): B = fold({ error("Unexpected failure: $it") }, { it })
