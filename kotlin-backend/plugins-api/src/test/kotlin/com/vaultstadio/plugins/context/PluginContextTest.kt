/**
 * VaultStadio Plugin Context Tests
 *
 * Unit tests for EndpointResponse factory methods and AIResult behavior.
 */

package com.vaultstadio.plugins.context

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PluginContextTest {

    @Nested
    inner class EndpointRequestTests {

        @Test
        fun `EndpointRequest holds all fields`() {
            val req = EndpointRequest(
                method = "GET",
                path = "/foo",
                headers = mapOf("A" to listOf("b")),
                queryParams = emptyMap(),
                body = null,
                userId = "u1",
            )
            assertEquals("GET", req.method)
            assertEquals("/foo", req.path)
            assertEquals(1, req.headers.size)
            assertEquals("u1", req.userId)
        }
    }

    @Nested
    inner class EndpointResponseTests {

        @Test
        fun `ok returns 200 with optional body`() {
            val r = EndpointResponse.ok()
            assertEquals(200, r.statusCode)
            assertNull(r.body)
            val r2 = EndpointResponse.ok("data")
            assertEquals(200, r2.statusCode)
            assertEquals("data", r2.body)
        }

        @Test
        fun `created returns 201`() {
            val r = EndpointResponse.created("id")
            assertEquals(201, r.statusCode)
            assertEquals("id", r.body)
        }

        @Test
        fun `noContent returns 204`() {
            val r = EndpointResponse.noContent()
            assertEquals(204, r.statusCode)
        }

        @Test
        fun `badRequest returns 400 with message`() {
            val r = EndpointResponse.badRequest("invalid")
            assertEquals(400, r.statusCode)
            assertEquals("""{"error": "invalid"}""", r.body)
        }

        @Test
        fun `notFound returns 404 with default message`() {
            val r = EndpointResponse.notFound()
            assertEquals(404, r.statusCode)
            assertEquals("""{"error": "Not found"}""", r.body)
        }

        @Test
        fun `notFound with custom message`() {
            val r = EndpointResponse.notFound("Missing resource")
            assertEquals(404, r.statusCode)
            assertEquals("""{"error": "Missing resource"}""", r.body)
        }

        @Test
        fun `error returns 500`() {
            val r = EndpointResponse.error("server error")
            assertEquals(500, r.statusCode)
            assertEquals("""{"error": "server error"}""", r.body)
        }
    }

    @Nested
    inner class HttpResponseTests {

        @Test
        fun `HttpResponse holds status headers and body`() {
            val r = HttpResponse(
                statusCode = 201,
                headers = mapOf("Location" to listOf("/x")),
                body = "created",
            )
            assertEquals(201, r.statusCode)
            assertEquals(listOf("/x"), r.headers["Location"])
            assertEquals("created", r.body)
        }
    }

    @Nested
    inner class AIResultTests {

        @Test
        fun `Success getOrNull returns data`() {
            val r = com.vaultstadio.plugins.context.AIResult.Success("value")
            assertEquals("value", r.getOrNull())
        }

        @Test
        fun `Error getOrNull returns null`() {
            val r = com.vaultstadio.plugins.context.AIResult.Error("fail")
            assertNull(r.getOrNull())
        }

        @Test
        fun `Success isSuccess is true`() {
            val r = com.vaultstadio.plugins.context.AIResult.Success(1)
            assertEquals(true, r.isSuccess())
        }

        @Test
        fun `Error isSuccess is false`() {
            val r = com.vaultstadio.plugins.context.AIResult.Error("x")
            assertEquals(false, r.isSuccess())
        }

        @Test
        fun `map on Success transforms data`() {
            val r = com.vaultstadio.plugins.context.AIResult.Success(2)
            val mapped = r.map { it * 2 }
            assertEquals(4, (mapped as com.vaultstadio.plugins.context.AIResult.Success).data)
        }

        @Test
        fun `map on Error returns same error`() {
            val r = com.vaultstadio.plugins.context.AIResult.Error("x")
            val mapped = r.map { it.toString() }
            assert(mapped is com.vaultstadio.plugins.context.AIResult.Error)
        }

        @Test
        fun `fold on Success calls onSuccess`() {
            val r = com.vaultstadio.plugins.context.AIResult.Success("a")
            val out = r.fold(
                onSuccess = { it + "!" },
                onError = { "err" },
            )
            assertEquals("a!", out)
        }

        @Test
        fun `fold on Error calls onError`() {
            val r = com.vaultstadio.plugins.context.AIResult.Error("msg", "code")
            val out = r.fold(
                onSuccess = { "ok" },
                onError = { "${it.message}:${it.code}" },
            )
            assertEquals("msg:code", out)
        }
    }
}
