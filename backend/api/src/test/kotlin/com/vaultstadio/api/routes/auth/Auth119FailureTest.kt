package com.vaultstadio.api.routes.auth

import com.vaultstadio.domain.auth.model.UserStatus
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

internal class Auth119FailureTest : Auth119HttpBase() {
    @Test fun `AUTH119-20 deactivated current user cannot authorize an existing session`() = app {
        client.register()
        val token = client.login().text("token")
        val user = fixture.user()
        fixture.users.update(user.copy(status = UserStatus.SUSPENDED)).fold({ throw it }, {})
        val response = client.me(token)
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(0, fixture.storageCount())
    }

    @Test fun `AUTH119-08 inactive identity cannot login or create a session`() = app {
        client.register()
        fixture.users.update(fixture.user().copy(status = UserStatus.SUSPENDED)).fold({ throw it }, {})
        assertSafeFailure(
            client.send("login", """{"email":"owner119@audit.invalid","password":"Audit119!Pass2026"}"""),
            HttpStatusCode.Forbidden,
        )
        assertEquals(0, fixture.sessionCount())
    }

    @Test fun `AUTH119-14 deactivated user cannot refresh the previously issued session`() = app {
        client.register()
        val login = client.login()
        fixture.users.update(fixture.user().copy(status = UserStatus.SUSPENDED)).fold({ throw it }, {})
        assertSafeFailure(
            client.send("refresh", """{"refreshToken":"${login.text("refreshToken")}"}"""),
            HttpStatusCode.Forbidden,
        )
        assertEquals(fixture.tokenHash(login.text("token")), fixture.session(login.text("token")).tokenHash)
    }

    @Test fun `AUTH119-21 expired session cannot authorize and its row is cleaned`() = app {
        client.register()
        val token = client.login().text("token")
        val session = fixture.session(token)
        fixture.sessions.update(session.copy(expiresAt = Clock.System.now() - 1.days)).fold({ throw it }, {})
        assertEquals(HttpStatusCode.Unauthorized, client.me(token).status)
        assertEquals(0, fixture.sessionCount())
    }

    @Test fun `AUTH119-13 expired refresh cannot rotate and cleans the expired session`() = app {
        client.register()
        val login = client.login()
        val session = fixture.session(login.text("token"))
        fixture.sessions.update(session.copy(expiresAt = Clock.System.now() - 1.days)).fold({ throw it }, {})
        assertSafeFailure(
            client.send("refresh", """{"refreshToken":"${login.text("refreshToken")}"}"""),
            HttpStatusCode.Unauthorized,
        )
        assertEquals(0, fixture.sessionCount())
    }

    @Test fun `AUTH119-18 failed SQL revocation must not return completed logout`() = app {
        client.register()
        val token = client.login().text("token")
        fixture.sessionsUnavailable {
            assertSafeFailure(client.send("logout", "", token), HttpStatusCode.InternalServerError)
        }
        assertEquals(1, fixture.sessionCount())
        assertEquals(HttpStatusCode.OK, client.me(token).status)
    }

    @Test fun `AUTH119-19 failed session persistence does not return login credentials`() = app {
        client.register()
        fixture.sessionsUnavailable {
            val response = client.send("login", """{"email":"owner119@audit.invalid","password":"Audit119!Pass2026"}""")
            assertSafeFailure(response, HttpStatusCode.InternalServerError)
            assertTrue(envelope(response)["data"] in listOf(null, JsonNull))
            assertEquals("DATABASE_ERROR", envelope(response).getValue("error").jsonObject.text("code"))
        }
        assertEquals(0, fixture.sessionCount())
    }
}
