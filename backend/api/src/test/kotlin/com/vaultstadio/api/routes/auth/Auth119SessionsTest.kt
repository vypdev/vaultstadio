package com.vaultstadio.api.routes.auth

import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class Auth119SessionsTest : Auth119HttpBase() {
    @Test fun `AUTH119-05 login returns opaque tokens and SQL retains only their hashes`() = app {
        client.register(); val login = client.login()
        val token = login.text("token"); val refresh = login.text("refreshToken")
        val session = fixture.session(token)
        assertTrue(token != refresh, "Distinct opaque credentials are required")
        assertTrue(token != session.tokenHash); assertTrue(refresh != session.refreshTokenHash)
        assertEquals(fixture.tokenHash(token), session.tokenHash)
        assertEquals(fixture.tokenHash(refresh), session.refreshTokenHash)
        assertEquals(1, fixture.sessionCount())
        val me = client.me(token)
        assertEquals(HttpStatusCode.OK, me.status)
        assertEquals(fixture.user().id, envelope(me).getValue("data").jsonObject.text("id"))
        assertEquals("owner119@audit.invalid", envelope(me).getValue("data").jsonObject.text("email"))
    }
    @Test fun `AUTH119-11 rotation invalidates both old credentials and persists one session`() = app {
        client.register(); val original = client.login()
        val response = client.send("refresh", """{"refreshToken":"${original.text("refreshToken")}"}""")
        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        val rotated = envelope(response).getValue("data").jsonObject
        assertTrue(original.text("token") != rotated.text("token"), "Access credential must rotate")
        assertTrue(original.text("refreshToken") != rotated.text("refreshToken"), "Refresh credential must rotate")
        assertEquals(1, fixture.sessionCount())
        assertEquals(HttpStatusCode.Unauthorized, client.me(original.text("token")).status)
        assertEquals(HttpStatusCode.OK, client.me(rotated.text("token")).status)
        assertSafeFailure(client.send("refresh", """{"refreshToken":"${original.text("refreshToken")}"}"""), HttpStatusCode.Unauthorized)
    }
    @Test fun `AUTH119-16 logout revokes the actual SQL session and protected access`() = app {
        client.register(); val token = client.login().text("token")
        assertEquals(HttpStatusCode.OK, client.send("logout", "", token).status)
        assertEquals(0, fixture.sessionCount())
        assertEquals(HttpStatusCode.Unauthorized, client.me(token).status)
        assertEquals(HttpStatusCode.OK, client.send("logout", "", token).status)
    }
    @Test fun `AUTH119-17 anonymous and unknown-session logout remain idempotent`() = app {
        assertEquals(HttpStatusCode.OK, client.send("logout", "").status)
        assertEquals(HttpStatusCode.OK, client.send("logout", "", "owned-invalid-token").status)
        assertEquals(0, fixture.sessionCount())
    }
    @Test fun `AUTH119-09 invalid and absent bearer cannot reveal an identity`() = app {
        assertEquals(HttpStatusCode.Unauthorized, client.me().status)
        assertEquals(HttpStatusCode.Unauthorized, client.me("owned-invalid-token").status)
        assertEquals(0, fixture.userCount()); assertEquals(0, fixture.sessionCount())
    }
    @Test fun `AUTH119-13 unknown refresh cannot create a session`() = app {
        assertSafeFailure(client.send("refresh", """{"refreshToken":"owned-unknown-refresh"}"""), HttpStatusCode.Unauthorized)
        assertEquals(0, fixture.sessionCount())
    }
    @Test fun `AUTH119-07 wrong password and unknown identity have the same safe outcome`() = app {
        client.register()
        val wrong = client.send("login", """{"email":"owner119@audit.invalid","password":"wrong-password"}""")
        val absent = client.send("login", """{"email":"absent119@audit.invalid","password":"wrong-password"}""")
        assertSafeFailure(wrong, HttpStatusCode.Unauthorized); assertSafeFailure(absent, HttpStatusCode.Unauthorized)
        assertEquals(wrong.bodyAsText(), absent.bodyAsText())
        assertEquals(0, fixture.sessionCount())
    }
    @Test fun `AUTH119-06 login records actual client metadata and returns current identity`() = app {
        client.register()
        val response = client.send("login", """{"email":"owner119@audit.invalid","password":"Audit119!Pass2026"}""", userAgent = "VaultStadioAudit119")
        assertEquals(HttpStatusCode.OK, response.status)
        val result = envelope(response).getValue("data").jsonObject
        val session = fixture.session(result.text("token"))
        assertEquals("VaultStadioAudit119", session.userAgent)
        assertEquals(fixture.user().id, session.userId)
        assertEquals(fixture.user().id, result.getValue("user").jsonObject.text("id"))
        assertTrue(!result.toString().contains(AUTH119_PASSWORD)); assertTrue(!result.toString().contains(fixture.user().passwordHash))
    }
    @Test fun `AUTH119-04 missing login and refresh fields cannot mutate session state`() = app {
        assertSafeFailure(client.send("login", "{}"), HttpStatusCode.BadRequest)
        assertSafeFailure(client.send("refresh", "{}"), HttpStatusCode.BadRequest)
        assertEquals(0, fixture.userCount()); assertEquals(0, fixture.sessionCount())
    }

}
