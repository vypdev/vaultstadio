package com.vaultstadio.app.data.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Network119RequestsTest {
    @Test fun net119BodylessCommandsNeverInventAnEntity() = runTest {
        val methods = mutableListOf<HttpMethod>()
        val engine = MockEngine { request ->
            assertIs<OutgoingContent.NoContent>(request.body)
            methods += request.method
            respond(NETWORK119_OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        withNetwork119Client(engine) { client ->
            val api = Network119Probe(client)
            api.read()
            api.command()
            api.remove()
            api.removeUnit()
        }
        assertEquals(listOf(HttpMethod.Get, HttpMethod.Post, HttpMethod.Delete, HttpMethod.Delete), methods)
    }

    @Test fun net119MethodsAndEncodedQueryReachTheProductionEngine() = runTest {
        val methods = mutableListOf<HttpMethod>()
        val engine = MockEngine { request ->
            assertEquals("audit.invalid", request.url.host)
            assertEquals("/record", request.url.encodedPath)
            methods += request.method
            if (request.method == HttpMethod.Get) assertEquals("é + &", request.url.parameters["q"])
            if (request.method == HttpMethod.Delete) assertEquals("true", request.url.parameters["permanent"])
            respond(NETWORK119_OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        withNetwork119Client(engine) { client ->
            val api = Network119Probe(client)
            api.read(mapOf("q" to "é + &"))
            api.create()
            api.update()
            api.command()
            api.removeUnit()
        }
        assertEquals(
            listOf(HttpMethod.Get, HttpMethod.Post, HttpMethod.Patch, HttpMethod.Post, HttpMethod.Delete),
            methods,
        )
    }

    @Test fun net119BearerIsReadPerRequestIncludingRemoval() = runTest {
        var token: String? = "first-ephemeral"
        val observed = mutableListOf<String?>()
        val engine = MockEngine { request ->
            observed += request.headers[HttpHeaders.Authorization]
            respond(NETWORK119_OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        withNetwork119Client(engine, TokenProvider { token }) { client ->
            val api = Network119Probe(client)
            api.read()
            token = "rotated-ephemeral"
            api.read()
            token = null
            api.read()
        }
        assertEquals(listOf("Bearer first-ephemeral", "Bearer rotated-ephemeral", null), observed)
    }

    @Test fun net119PostAndPatchSerializeActualBody() = runTest {
        val bodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            bodies += (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            respond(NETWORK119_OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        withNetwork119Client(engine) { client ->
            Network119Probe(client).create()
            Network119Probe(client).update()
        }
        assertEquals("sent", HttpClientFactory.getJson().decodeFromString<Network119Record>(bodies[0]).id)
        assertEquals("é + &", HttpClientFactory.getJson().decodeFromString<Network119Record>(bodies[0]).name)
        assertEquals("changed", HttpClientFactory.getJson().decodeFromString<Network119Record>(bodies[1]).id)
    }

    @Test fun net119FormEncodingDoesNotAlterReservedCharacters() = runTest {
        var body = ""
        val engine = MockEngine { request ->
            body = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            respond(NETWORK119_OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        withNetwork119Client(engine) { client ->
            Network119Probe(client).form()
        }
        assertTrue(body.contains("%2B"))
        assertTrue(body.contains("%26"))
        assertFalse(body.contains("é"))
    }
}
