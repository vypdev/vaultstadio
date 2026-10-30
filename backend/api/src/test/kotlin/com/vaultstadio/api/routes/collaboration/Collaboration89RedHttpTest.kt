package com.vaultstadio.api.routes.collaboration

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.test.assertEquals

class Collaboration89RedHttpTest {
    @Test
    fun `foreign join is forbidden and cannot persist membership`() {
        val f = CollaborationIntegrationFixture()
        try {
            f.start()
            f.reset()
            testApplication {
                application { f.install(this) }
                val path = "/api/v1/collaboration/sessions/join"
                val body = """{"itemId":"${f.item.id}"}"""
                assertEquals(
                    HttpStatusCode.Created,
                    client.post(path) {
                        bearerAuth("owner")
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }.status,
                )
                val before = f.snapshot()
                val response = client.post(path) {
                    bearerAuth("unrelated")
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                assertAll(
                    { assertEquals(HttpStatusCode.Forbidden, response.status) },
                    { assertEquals(before, f.snapshot()) },
                )
            }
        } finally {
            f.close()
        }
    }
}
