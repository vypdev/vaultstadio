package com.vaultstadio.api.routes.auth

import arrow.core.Either
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.StorageException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

/** Real SQL state and provider faults, complementary to credential-facing HTTP journeys. */
internal class Auth119SessionPersistenceTest : Auth119HttpBase() {
    @Test fun `AUTH119-19 persisted session reads updates and account revocation preserve identity boundaries`() = app {
        client.register()
        val token = client.login().text("token")
        val before = fixture.session(token)
        assertEquals(before, fixture.sessions.findById(before.id).fold({ throw it }, { it }))
        assertEquals(listOf(before), fixture.sessions.findByUserId(before.userId).fold({ throw it }, { it }))
        assertTrue(fixture.sessions.findByUserId("unknown-owner119").fold({ throw it }, { it }).isEmpty())
        fixture.sessions.deleteByUserId("unknown-owner119").fold({ throw it }, {})
        assertEquals(before, fixture.session(token))
        val updated = before.copy(tokenHash = fixture.tokenHash("owned-updated119"))
        fixture.sessions.update(updated).fold({ throw it }, {})
        assertNull(fixture.sessions.findByTokenHash(before.tokenHash).fold({ throw it }, { it }))
        assertEquals(updated, fixture.session("owned-updated119"))
        fixture.sessions.updateLastActivity("unknown-session119").fold({ throw it }, {})
        assertEquals(1, fixture.sessionCount())
        fixture.sessions.deleteByUserId(before.userId).fold({ throw it }, {})
        assertEquals(0, fixture.sessionCount())
        assertNull(fixture.sessions.findById(before.id).fold({ throw it }, { it }))
    }

    @Test fun `AUTH119-21 expiry cleanup removes only expired sessions and preserves active credentials`() = app {
        client.register()
        val token = client.login().text("token")
        val active = fixture.session(token)
        val expired = active.copy(
            id = "owned-expired119",
            tokenHash = fixture.tokenHash("owned-expired-access119"),
            refreshTokenHash = fixture.tokenHash("owned-expired-refresh119"),
            expiresAt = active.createdAt - 1.days,
        )
        fixture.sessions.create(expired).fold({ throw it }, {})
        assertEquals(1, fixture.sessions.deleteExpired().fold({ throw it }, { it }))
        assertNull(fixture.sessions.findById(expired.id).fold({ throw it }, { it }))
        assertEquals(active, fixture.session(token))
        assertEquals(0, fixture.sessions.deleteExpired().fold({ throw it }, { it }))
        assertEquals(1, fixture.sessionCount())
    }

    @Test fun `AUTH119-19 every session provider fault remains a database error without fabricated state`() = app {
        client.register()
        val token = client.login().text("token")
        val before = fixture.session(token)
        fixture.sessionsUnavailable {
            val operations: List<suspend () -> Either<StorageException, *>> = listOf(
                { fixture.sessions.create(before) },
                { fixture.sessions.findById(before.id) },
                { fixture.sessions.findByTokenHash(before.tokenHash) },
                { fixture.sessions.findByRefreshTokenHash(checkNotNull(before.refreshTokenHash)) },
                { fixture.sessions.findByUserId(before.userId) },
                { fixture.sessions.update(before) },
                { fixture.sessions.rotate(before, checkNotNull(before.refreshTokenHash)) },
                { fixture.sessions.delete(before.id) },
                { fixture.sessions.deleteByUserId(before.userId) },
                { fixture.sessions.deleteExpired() },
                { fixture.sessions.updateLastActivity(before.id) },
            )
            for (operation in operations) {
                assertIs<DatabaseException>(assertIs<Either.Left<*>>(operation()).value)
            }
        }
        assertEquals(before, fixture.session(token))
        assertEquals(1, fixture.sessionCount())
    }
}
