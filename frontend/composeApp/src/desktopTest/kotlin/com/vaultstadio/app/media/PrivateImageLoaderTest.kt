package com.vaultstadio.app.media

import coil3.PlatformContext
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.network.HttpClientFactory
import com.vaultstadio.app.data.network.TokenProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Actual Coil fetch/decode path with the application transport, not a URL-only simulation. */
class PrivateImageLoaderTest {
    @Test fun api116ImagesRevalidateCredentialsWithoutReusingAnotherSessionCache() = runBlocking<Unit> {
        var token: String? = "first-session"
        val seen = mutableListOf<String?>()
        val engine = MockEngine { request ->
            seen += request.headers[HttpHeaders.Authorization]
            assertEquals("/gateway/api/v1/storage/item/image/thumbnail", request.url.encodedPath)
            assertTrue(request.headers[HttpHeaders.CacheControl]!!.contains("no-store"))
            if (request.headers[HttpHeaders.Authorization] == null) {
                respond("denied", HttpStatusCode.Unauthorized)
            } else {
                respond(png(), headers = headersOf(HttpHeaders.ContentType, "image/png"))
            }
        }
        val client = HttpClientFactory.create(
            ApiClientConfig("https://audit.invalid/gateway/api"),
            TokenProvider {
                token
            },
            engine,
        )
        val loader = privateImageLoader(PlatformContext.INSTANCE, client)
        try {
            repeat(3) { index ->
                token = listOf("first-session", "second-session", null)[index]
                val result = loader.execute(
                    ImageRequest.Builder(PlatformContext.INSTANCE)
                        .data("https://audit.invalid/gateway/api/v1/storage/item/image/thumbnail").build(),
                )
                if (index < 2) {
                    assertEquals(1, assertIs<SuccessResult>(result).image.width)
                } else {
                    assertIs<ErrorResult>(result)
                }
            }
            assertEquals(listOf("Bearer first-session", "Bearer second-session", null), seen)
        } finally {
            loader.shutdown()
            client.close()
            engine.close()
        }
    }

    @Test fun api116ForeignImageDoesNotReceiveAccountBearer() = runBlocking<Unit> {
        val engine = MockEngine { request ->
            assertEquals("foreign.invalid", request.url.host)
            assertNull(request.headers[HttpHeaders.Authorization])
            respond("invalid", headers = headersOf(HttpHeaders.ContentType, "image/png"))
        }
        val client = HttpClientFactory.create(
            ApiClientConfig("https://audit.invalid/gateway/api"),
            TokenProvider {
                "private-session"
            },
            engine,
        )
        val loader = privateImageLoader(PlatformContext.INSTANCE, client)
        try {
            assertIs<ErrorResult>(
                loader.execute(
                    ImageRequest.Builder(PlatformContext.INSTANCE)
                        .data("https://foreign.invalid/image.png").build(),
                ),
            )
        } finally {
            loader.shutdown()
            client.close()
            engine.close()
        }
    }

    private fun png() = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/iZk9HQAAAABJRU5ErkJggg==",
    )
}
