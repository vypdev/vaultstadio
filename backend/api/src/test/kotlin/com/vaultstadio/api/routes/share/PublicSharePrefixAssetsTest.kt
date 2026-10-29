package com.vaultstadio.api.routes.share

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PublicSharePrefixAssetsTest {
    @Test fun `API116 prefixed GET and rejected deeper POST retain working relative assets`() = testApplication {
        application { routing { route("/api/v1") { publicSharePageRoutes() } } }
        val get = client.get("/api/v1/share/CaseToken/view")
        val post = client.post("/api/v1/share/CaseToken/view/download") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.OK, get.status)
        assertEquals(HttpStatusCode.BadRequest, post.status)
        for ((response, suffix) in listOf(get to "/view", post to "/view/download")) {
            val html = response.bodyAsText()
            assertFalse(html.contains("{{"))
            assertTrue(response.headers["Content-Security-Policy"]!!.contains("base-uri 'none'"))
            assertEquals("strict-origin", response.headers["Referrer-Policy"])
            val assets = Regex("(?:src|href)=\"([^\"]+)\"").findAll(html).map { it.groupValues[1] }.toList()
            assertEquals(2, assets.size)
            for (prefix in listOf("/api", "/gateway/api", "/tenant%20one/api")) {
                val page = URI("https://viewer.invalid$prefix/v1/share/CaseToken$suffix")
                for (asset in assets) {
                    val resolved = page.resolve(asset)
                    assertEquals("viewer.invalid", resolved.host)
                    assertEquals("$prefix/v1/share-assets/${asset.substringAfterLast('/')}", resolved.rawPath)
                }
            }
        }
        val css = client.get("/api/v1/share-assets/public-share.css")
        val js = client.get("/api/v1/share-assets/public-share.mjs")
        assertEquals(HttpStatusCode.OK, css.status)
        assertEquals(HttpStatusCode.OK, js.status)
        assertTrue(css.contentType()!!.match(ContentType.Text.CSS))
        assertTrue(js.contentType()!!.match(ContentType.Application.JavaScript))
    }
}
