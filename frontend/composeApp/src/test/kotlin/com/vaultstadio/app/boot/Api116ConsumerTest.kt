package com.vaultstadio.app.boot

import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.network.HttpClientFactory
import com.vaultstadio.app.data.network.TokenProvider
import com.vaultstadio.app.data.share.api.ShareApi
import com.vaultstadio.app.data.share.repository.ShareRepositoryImpl
import com.vaultstadio.app.data.share.service.ShareService
import com.vaultstadio.app.data.version.api.VersionApi
import com.vaultstadio.app.data.version.repository.VersionRepositoryImpl
import com.vaultstadio.app.data.version.service.VersionService
import com.vaultstadio.app.domain.result.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class Api116ConsumerTest {
    @Test fun listedAndCreatedSharesUseConfiguredPublicPageEvenIfServerUrlIsInternal() = runTest {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            paths += request.url.encodedPath
            assertEquals("Bearer local-session", request.headers[HttpHeaders.Authorization])
            val value = dto("CaseToken_-")
            val body = if (request.method.value == "POST") value else "[$value]"
            respond(
                "{\"success\":true,\"data\":$body}",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val config = ApiClientConfig("https://viewer.invalid/gateway/api")
        val client = HttpClientFactory.create(config, TokenProvider { "local-session" }, engine)
        val repo = ShareRepositoryImpl(ShareService(ShareApi(client)), config)
        try {
            val my = assertIs<Result.Success<*>>(repo.getMyShares()).data as List<*>
            val shared = repo.getSharedWithMe().getOrThrow()
            val created = repo.createShare("item", null, null, null).getOrThrow()
            assertEquals(created, my.single())
            assertEquals(created, shared.single())
            assertEquals("https://viewer.invalid/gateway/api/v1/share/CaseToken_-/view", created.url)
            assertEquals(
                listOf("/gateway/api/v1/shares", "/gateway/api/v1/shares/shared-with-me", "/gateway/api/v1/shares"),
                paths,
            )
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun malformedShareTokenRejectsTheWholeListWithoutPublishingAnInternalUrl() = runTest {
        for (token in listOf("", "a/b", "a?b", "x".repeat(65))) {
            val engine = MockEngine {
                respond(
                    "{\"success\":true,\"data\":[${dto("valid")},${dto(token)}]}",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            val config = ApiClientConfig("https://viewer.invalid/gateway/api")
            val client = HttpClientFactory.create(config, TokenProvider { null }, engine)
            try {
                val result = ShareRepositoryImpl(ShareService(ShareApi(client)), config).getMyShares()
                assertEquals("INVALID_SHARE_RESPONSE", assertIs<Result.Error>(result).code)
            } finally {
                client.close()
                engine.close()
            }
        }
    }

    @Test fun serverFailureRetainsItsResultInsteadOfBecomingALink() = runTest {
        val engine =
            MockEngine {
                respond("{}", HttpStatusCode.Forbidden, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        val config = ApiClientConfig("https://viewer.invalid/gateway/api")
        val client = HttpClientFactory.create(config, TokenProvider { null }, engine)
        try {
            assertIs<Result.Error>(ShareRepositoryImpl(ShareService(ShareApi(client)), config).getMyShares())
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun historicalLocationMatchesTheMetadataRouteAndCannotContainAccountCredentials() {
        val config = ApiClientConfig("https://viewer.invalid/gateway/api/v1/")
        val engine = MockEngine { error("location generation must not perform IO") }
        val client = HttpClientFactory.create(config, TokenProvider { "must-never-appear" }, engine)
        try {
            val repo = VersionRepositoryImpl(VersionService(VersionApi(client)), config)
            val url = repo.getVersionDownloadUrl("a/b? é", 7)
            assertEquals(
                "https://viewer.invalid/gateway/api/v1/versions/item/a%2Fb%3F%20%C3%A9/version/7/download",
                url,
            )
            assertFalse(url.contains("token="))
            // This route returns metadata today; this assertion does not certify historical binary download (#109).
        } finally {
            client.close()
            engine.close()
        }
    }
    private fun dto(
        token: String,
    ) = """{"id":"share","itemId":"item","token":${JsonPrimitive(
        token,
    )},"url":"http://internal.invalid/api/v1/share/old","expiresAt":null,"hasPassword":false,"maxDownloads":null,"downloadCount":0,"isActive":true,"createdAt":"2026-10-29T00:00:00Z"}"""
}
