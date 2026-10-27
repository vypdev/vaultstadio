package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.core.domain.model.PresenceStatus
import com.vaultstadio.core.domain.model.UserPresence
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.DatabaseException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class CollaborationPresenceContractTest {
    @ParameterizedTest(name = "C89-PRES {0}")
    @ValueSource(
        strings = [
            "read-self",
            "read-empty",
            "read-foreign",
            "read-mixed",
            "read-wrong-key",
            "read-wrong-user",
            "read-foreign-document",
            "read-foreign-session",
            "update-self",
            "update-owned-document",
            "update-owned-session",
            "update-cross-bound-pointers",
            "offline-self",
            "offline-blank",
            "subscribe-self",
            "subscribe-foreign",
            "subscribe-wrong-user",
            "subscribe-foreign-pointer",
        ],
    )
    fun `global presence cannot cross actor or active resource scope`(case: String) = runTest {
        val f = CollaborationServiceFixture()
        val self = UserPresence("user-1", PresenceStatus.ONLINE, f.now)
        arrange(f, case, self)
        val result = invoke(f, case)
        checkOutcome(f, case, result, self)
        verifyEffects(f, case)
    }

    private fun arrange(f: CollaborationServiceFixture, case: String, self: UserPresence) {
        val row = when (case) {
            "read-wrong-user", "subscribe-wrong-user" -> self.copy(userId = "foreign")
            "read-foreign-document", "subscribe-foreign-pointer" -> self.copy(activeDocument = f.item.id)
            "read-foreign-session" -> self.copy(activeSession = f.session.id)
            else -> self
        }
        if (case in setOf("read-foreign-document", "read-foreign-session", "subscribe-foreign-pointer")) {
            f.item = f.item.copy(ownerId = "other")
        }
        val key = if (case == "read-wrong-key") "foreign" else "user-1"
        coEvery { f.repository.getPresenceBulk(any()) } returns mapOf(key to row).right()
        every { f.repository.streamPresence(any()) } returns flowOf(row)
    }

    private fun verifyEffects(f: CollaborationServiceFixture, case: String) {
        if (case == "read-empty") coVerify(exactly = 0) { f.repository.getPresenceBulk(any()) }
        if (case == "subscribe-foreign") verify(exactly = 0) { f.repository.streamPresence(any()) }
        if (case == "offline-self") coVerify(exactly = 1) { f.repository.setOffline("user-1") }
        if (case == "update-cross-bound-pointers") coVerify(exactly = 0) { f.repository.updatePresence(any()) }
    }

    private suspend fun checkOutcome(
        f: CollaborationServiceFixture,
        case: String,
        result: Either<*, *>,
        self: UserPresence,
    ) {
        when (case) {
            "read-foreign",
            "read-mixed",
            "read-foreign-document",
            "read-foreign-session",
            "update-cross-bound-pointers",
            "offline-blank",
            "subscribe-foreign",
            -> {
                assertIs<AuthorizationException>(assertIs<Either.Left<*>>(result).value)
            }
            "read-wrong-key", "read-wrong-user" -> {
                assertIs<DatabaseException>(assertIs<Either.Left<*>>(result).value)
            }
            "subscribe-wrong-user" -> {
                val subscription = f.service.streamPresence(listOf("user-1"), "user-1").requiredPresence()
                assertFailsWith<DatabaseException> { subscription.toList() }
            }
            "subscribe-foreign-pointer" -> {
                val subscription = f.service.streamPresence(listOf("user-1"), "user-1").requiredPresence()
                assertFailsWith<AuthorizationException> { subscription.toList() }
            }
            "subscribe-self" -> {
                val subscription = f.service.streamPresence(listOf("user-1"), "user-1").requiredPresence()
                assertEquals(listOf(self), subscription.toList())
            }
            else -> {
                assertIs<Either.Right<*>>(result)
            }
        }
    }

    private suspend fun invoke(f: CollaborationServiceFixture, case: String): Either<*, *> = when (case) {
        "read-empty" -> f.service.getPresence(emptyList(), "user-1")
        "read-foreign" -> f.service.getPresence(listOf("foreign"), "user-1")
        "read-mixed" -> f.service.getPresence(listOf("user-1", "foreign"), "user-1")
        "update-self" -> f.service.updatePresence("user-1", PresenceStatus.ONLINE)
        "update-owned-document" -> f.service.updatePresence("user-1", PresenceStatus.ONLINE, activeDocument = f.item.id)
        "update-owned-session" -> f.service.updatePresence("user-1", PresenceStatus.ONLINE, f.session.id, f.item.id)
        "update-cross-bound-pointers" -> f.service.updatePresence(
            "user-1",
            PresenceStatus.ONLINE,
            f.session.id,
            "other-item",
        )
        "offline-self" -> f.service.setOffline("user-1")
        "offline-blank" -> f.service.setOffline(" ")
        "subscribe-foreign" -> f.service.streamPresence(listOf("foreign"), "user-1")
        "subscribe-self", "subscribe-wrong-user", "subscribe-foreign-pointer" -> f.service.streamPresence(
            listOf("user-1"),
            "user-1",
        )
        else -> f.service.getPresence(listOf("user-1"), "user-1")
    }
}

private fun <A : Throwable, B> Either<A, B>.requiredPresence(): B = fold({ throw it }, { it })
