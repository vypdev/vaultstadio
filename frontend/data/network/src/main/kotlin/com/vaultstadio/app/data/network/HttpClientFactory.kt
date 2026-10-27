/**
 * HTTP Client Factory
 *
 * Creates and configures the Ktor HttpClient for API communication.
 */

package com.vaultstadio.app.data.network

import com.vaultstadio.app.data.network.serialization.InstantIso8601Serializer
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlin.time.Instant

/**
 * API client configuration.
 */
data class ApiClientConfig(
    val baseUrl: String,
    val timeout: Long = 30_000,
)

/**
 * Token provider interface for authentication.
 */
fun interface TokenProvider {
    fun getToken(): String?
}

/**
 * Factory for creating configured HttpClient instances.
 */
object HttpClientFactory {

    private val json = Json {
        prettyPrint = false
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
        serializersModule = SerializersModule {
            contextual(Instant::class, InstantIso8601Serializer)
        }
    }

    /**
     * Creates a configured HttpClient.
     */
    fun create(
        config: ApiClientConfig,
        tokenProvider: TokenProvider,
        engine: HttpClientEngine? = null,
        logger: Logger? = null,
    ): HttpClient = if (engine == null) {
        HttpClient { configure(config, tokenProvider, logger) }
    } else {
        HttpClient(engine) { configure(config, tokenProvider, logger) }
    }

    private fun HttpClientConfig<*>.configure(config: ApiClientConfig, tokenProvider: TokenProvider, logger: Logger?) {
        install(ContentNegotiation) {
            json(json)
        }

        install(Logging) {
            level = LogLevel.INFO
            filter { !it.url.encodedPath.startsWith("/api/v1/share/") }
            sanitizeHeader { it.equals(HttpHeaders.Authorization, ignoreCase = true) }
            if (logger != null) this.logger = logger
        }

        install(HttpTimeout) {
            requestTimeoutMillis = config.timeout
            connectTimeoutMillis = config.timeout
        }

        defaultRequest {
            url(config.baseUrl)
            contentType(ContentType.Application.Json)
            tokenProvider.getToken()?.let {
                headers.append(HttpHeaders.Authorization, "Bearer $it")
            }
        }
    }

    /**
     * Gets the JSON serializer configuration.
     */
    fun getJson(): Json = json
}
