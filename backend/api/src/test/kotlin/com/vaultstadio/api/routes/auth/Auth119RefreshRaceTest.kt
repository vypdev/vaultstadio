package com.vaultstadio.api.routes.auth

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Only timing at the repository port is controlled; reads, rotations, routes and SQL are real. */
internal class Auth119RefreshRaceTest : Auth119HttpBase() {
    @Test fun `AUTH119-11 one old refresh credential has exactly one concurrent winner`() = app {
        client.register()
        val login = client.login()
        val readers = AtomicInteger()
        val bothRead = CompletableDeferred<Unit>()
        fixture.afterRefreshRead = {
            if (readers.incrementAndGet() == 2) bothRead.complete(Unit)
            withTimeout(10_000) { bothRead.await() }
        }
        val body = """{"refreshToken":"${login.text("refreshToken")}"}"""
        val responses = coroutineScope { List(2) { async { client.send("refresh", body) } }.awaitAll() }
        assertEquals(1, responses.count { it.status == HttpStatusCode.OK })
        assertEquals(1, responses.count { it.status == HttpStatusCode.Unauthorized })
        assertEquals(1, fixture.sessionCount())
        val success = envelope(responses.single { it.status == HttpStatusCode.OK }).getValue("data").jsonObject
        assertEquals(HttpStatusCode.OK, client.me(success.text("token")).status)
        assertEquals(HttpStatusCode.Unauthorized, client.me(login.text("token")).status)
        fixture.afterRefreshRead = {}
        assertEquals(HttpStatusCode.Unauthorized, client.send("refresh", body).status)
    }

    @Test fun `AUTH119-12 refresh held across logout cannot claim an unpersisted session`() = app {
        client.register()
        val login = client.login()
        val read = CompletableDeferred<Unit>()
        val proceed = CompletableDeferred<Unit>()
        fixture.afterRefreshRead = {
            read.complete(Unit)
            withTimeout(10_000) { proceed.await() }
        }
        coroutineScope {
            val refresh = async { client.send("refresh", """{"refreshToken":"${login.text("refreshToken")}"}""") }
            withTimeout(10_000) { read.await() }
            try {
                assertEquals(HttpStatusCode.OK, client.send("logout", "", login.text("token")).status)
                assertEquals(0, fixture.sessionCount())
            } finally {
                proceed.complete(Unit)
            }
            assertEquals(HttpStatusCode.Unauthorized, refresh.await().status)
        }
        assertEquals(0, fixture.sessionCount())
        assertTrue(fixture.userCount() == 1L)
    }
}
