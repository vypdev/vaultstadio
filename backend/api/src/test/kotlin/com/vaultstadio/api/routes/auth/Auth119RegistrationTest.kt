package com.vaultstadio.api.routes.auth

import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class Auth119RegistrationTest : Auth119HttpBase() {
    @Test fun `AUTH119-01 real registration hashes password and persists USER without session`() = app {
        val result = client.register()
        val stored = fixture.user()
        assertEquals(stored.id, result.text("id"))
        assertEquals("USER", result.text("role"))
        assertEquals(1, fixture.userCount())
        assertEquals(0, fixture.sessionCount())
        assertNotEquals(AUTH119_PASSWORD, stored.passwordHash)
        assertTrue(fixture.hasher.verify(AUTH119_PASSWORD, stored.passwordHash))
        assertFalse(result.containsKey("password")); assertFalse(result.containsKey("passwordHash"))
    }
    @Test fun `AUTH119-02 duplicate email creates no second user`() = app {
        client.register()
        assertSafeFailure(client.send("register", AUTH119_REGISTER.replace("owner119\"", "other119\"")), HttpStatusCode.BadRequest)
        assertEquals(1, fixture.userCount()); assertEquals(0, fixture.sessionCount())
    }
    @Test fun `AUTH119-02 duplicate username creates no second user`() = app {
        client.register()
        assertSafeFailure(client.send("register", AUTH119_REGISTER.replace("owner119@audit.invalid", "other119@audit.invalid")), HttpStatusCode.BadRequest)
        assertEquals(1, fixture.userCount())
    }
    @ParameterizedTest @ValueSource(strings = ["invalid", "no-at.invalid", "@audit.invalid", "", "user@", "user@@audit.invalid", "user@.invalid", "user@audit.", "user@audit..invalid", "user @audit.invalid"])
    fun `AUTH119-03 invalid email has zero persisted effects`(email: String) = app {
        assertSafeFailure(client.send("register", AUTH119_REGISTER.replace("owner119@audit.invalid", email)), HttpStatusCode.BadRequest)
        assertEquals(0, fixture.userCount()); assertEquals(0, fixture.sessionCount())
    }
    @ParameterizedTest @ValueSource(strings = ["weak", "12345678", "abcdefgh", "a1short"])
    fun `AUTH119-03 password boundary rejects weak input without effects`(password: String) = app {
        assertSafeFailure(client.send("register", AUTH119_REGISTER.replace(AUTH119_PASSWORD, password)), HttpStatusCode.BadRequest)
        assertEquals(0, fixture.userCount()); assertEquals(0, fixture.sessionCount())
    }
    @ParameterizedTest @ValueSource(strings = ["{}", "{", """{"email":[],"username":"u","password":"a1b2c3d4"}"""])
    fun `AUTH119-04 malformed request is a client error`(body: String) = app {
        assertSafeFailure(client.send("register", body), HttpStatusCode.BadRequest)
        assertEquals(0, fixture.userCount()); assertEquals(0, fixture.sessionCount())
    }
    @Test fun `AUTH119-01 registration normalizes email while retaining identity`() = app {
        val result = client.register(AUTH119_REGISTER.replace("owner119@audit.invalid", "OWNER119@AUDIT.INVALID"))
        assertEquals("owner119@audit.invalid", result.text("email"))
        assertEquals("owner119", result.text("username"))
        assertEquals(1, fixture.userCount())
    }
    @Test fun `AUTH119-22 registration response never echoes plaintext or hashes`() = app {
        val response = client.send("register", AUTH119_REGISTER)
        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.bodyAsText()
        assertFalse(body.contains(AUTH119_PASSWORD)); assertFalse(body.contains(fixture.user().passwordHash))
    }
    @Test fun `AUTH119-01 public registration cannot request administrator role`() = app {
        val body = AUTH119_REGISTER.dropLast(1) + ",\"role\":\"ADMIN\"}"
        val user = client.register(body)
        assertEquals("USER", user.text("role"))
        assertEquals("USER", fixture.user().role.name)
    }
    @Test fun `AUTH119-01 supported plus tag and minimum password remain valid`() = app {
        val body = AUTH119_REGISTER.replace("owner119@audit.invalid", "Owner119+tag@Audit.Invalid").replace(AUTH119_PASSWORD, "a1b2c3d4")
        val user = client.register(body)
        assertEquals("owner119+tag@audit.invalid", user.text("email"))
        assertTrue(fixture.hasher.verify("a1b2c3d4", fixture.user("owner119+tag@audit.invalid").passwordHash))
    }

}
