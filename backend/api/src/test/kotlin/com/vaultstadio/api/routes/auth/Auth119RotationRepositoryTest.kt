package com.vaultstadio.api.routes.auth

import arrow.core.Either
import com.vaultstadio.domain.common.exception.DatabaseException
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

/** Direct infrastructure assertions complement the real HTTP concurrency tests. */
internal class Auth119RotationRepositoryTest : Auth119HttpBase() {
    @Test fun `AUTH119-11 conditional rotation commits both hashes while preserving session metadata`() = app {
        client.register()
        val token = client.login().text("token")
        val before = fixture.session(token)
        val next = before.copy(
            tokenHash = fixture.tokenHash("owned-next-access"),
            refreshTokenHash = fixture.tokenHash("owned-next-refresh"),
            expiresAt =
            before.expiresAt + 1.days,
        )
        val result = fixture.sessions.rotate(next, checkNotNull(before.refreshTokenHash)).fold({ throw it }, { it })
        assertTrue(result)
        assertEquals(next, fixture.session("owned-next-access"))
        assertEquals(1, fixture.sessionCount())
    }

    @Test fun `AUTH119-12 wrong expected credential leaves every session field unchanged`() = app {
        client.register()
        val token = client.login().text("token")
        val before = fixture.session(token)
        val next = before.copy(tokenHash = fixture.tokenHash("not-committed"))
        assertEquals(
            false,
            fixture.sessions.rotate(next, fixture.tokenHash("wrong-expectation")).fold({
                throw it
            }, { it }),
        )
        assertEquals(before, fixture.session(token))
    }

    @Test fun `AUTH119-12 absent session cannot be recreated through conditional rotation`() = app {
        client.register()
        val token = client.login().text("token")
        val before = fixture.session(token)
        fixture.sessions.delete(before.id).fold({ throw it }, {})
        assertEquals(
            false,
            fixture.sessions.rotate(
                before.copy(lastActivityAt = Clock.System.now()),
                checkNotNull(before.refreshTokenHash),
            ).fold({
                throw it
            }, { it }),
        )
        assertEquals(0, fixture.sessionCount())
    }

    @Test fun `AUTH119-19 conditional rotation preserves a real SQL failure and unchanged state`() = app {
        client.register()
        val token = client.login().text("token")
        val before = fixture.session(token)
        fixture.sessionsUnavailable {
            val result = fixture.sessions.rotate(
                before.copy(tokenHash = fixture.tokenHash("not-committed")),
                checkNotNull(before.refreshTokenHash),
            )
            val failure = assertIs<Either.Left<*>>(result).value
            assertIs<DatabaseException>(failure)
        }
        assertEquals(before, fixture.session(token))
    }
}
