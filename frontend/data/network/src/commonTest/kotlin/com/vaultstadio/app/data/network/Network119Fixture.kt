package com.vaultstadio.app.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.Serializable

@Serializable
internal data class Network119Record(val id: String, val name: String = "default")

internal class Network119Probe(client: HttpClient) : BaseApi(client) {
    suspend fun read(params: Map<String, String> = emptyMap()) = get<Network119Record>("/record", params)
    suspend fun create() = post<Network119Record, Network119Record>("/record", Network119Record("sent", "é + &"))
    suspend fun update() = patch<Network119Record, Network119Record>("/record", Network119Record("changed"))
    suspend fun remove() = delete<Network119Record>("/record")
    suspend fun command() = postNoBody<Network119Record>("/record")
    suspend fun removeUnit() = deleteWithParams<Unit>("/record", mapOf("permanent" to "true"))
    suspend fun form() = postForm<Network119Record>("/record", mapOf("name" to "é + &"))
}

internal const val NETWORK119_OK = """{"success":true,"data":{"id":"kept","name":"é + &"}}"""

internal suspend fun <T> withNetwork119Client(
    content: String = NETWORK119_OK,
    status: HttpStatusCode = HttpStatusCode.OK,
    block: suspend (HttpClient) -> T,
): T = withNetwork119Client(
    MockEngine { respond(content, status, headersOf(HttpHeaders.ContentType, "application/json")) },
    block = block,
)

internal suspend fun <T> withNetwork119Client(
    engine: MockEngine,
    tokenProvider: TokenProvider = TokenProvider { null },
    block: suspend (HttpClient) -> T,
): T {
    val client = HttpClientFactory.create(ApiClientConfig("https://audit.invalid"), tokenProvider, engine)
    try { return block(client) } finally { client.close(); engine.close() }
}
