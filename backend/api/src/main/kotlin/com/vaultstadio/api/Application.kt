/**
 * VaultStadio API Application
 *
 * Main entry point for the Ktor server.
 */

package com.vaultstadio.api

import com.vaultstadio.api.config.AppConfig
import com.vaultstadio.api.config.HttpServerConfig
import com.vaultstadio.api.config.configureKoin
import com.vaultstadio.api.config.configureRouting
import com.vaultstadio.api.config.configureSecurity
import com.vaultstadio.api.config.configureSerialization
import com.vaultstadio.api.config.configureSwagger
import com.vaultstadio.api.middleware.configureErrorHandling
import com.vaultstadio.api.middleware.configureLogging
import com.vaultstadio.api.plugins.CronScheduler
import com.vaultstadio.core.domain.service.UploadSessionManager
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.websocket.WebSockets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.time.Duration
import org.koin.ktor.ext.get as koinGet

private val logger = KotlinLogging.logger {}

/**
 * Application entry point.
 */
fun main(args: Array<String>) {
    val config = AppConfig.fromEnvironment()

    logger.info { "Starting VaultStadio API Server on ${config.server.host}:${config.server.port}" }
    logger.info { "Development mode: ${config.server.development}" }
    logger.info { "Database URL: ${config.database.url}" }
    logger.info { "Storage type: ${config.storage.type}" }

    embeddedServer(
        Netty,
        port = config.server.port,
        host = config.server.host,
    ) {
        module(config)
    }.start(wait = true)
}

/**
 * Main application module.
 */
fun Application.module(config: AppConfig? = null) {
    val appConfig = config ?: AppConfig.fromEnvironment()

    logger.info { "Initializing VaultStadio API modules..." }

    // Configure CORS
    configureCors(appConfig.server)

    // Configure dependency injection
    configureKoin(appConfig)

    // Configure serialization (JSON)
    configureSerialization()

    // WebSockets (for collaboration)
    install(WebSockets)

    // Configure security (JWT, authentication)
    configureSecurity()

    // Configure logging
    configureLogging()

    // Configure error handling
    configureErrorHandling()

    // Configure API routes
    configureRouting()

    // Configure Swagger/OpenAPI (development mode only)
    if (appConfig.server.development) {
        configureSwagger()
        logger.info { "Swagger UI available at /swagger" }
    }

    // Configure background jobs
    configureBackgroundJobs()

    logger.info { "VaultStadio API Server initialized successfully" }
}

/**
 * Configures background jobs for maintenance tasks.
 */
private fun Application.configureBackgroundJobs() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val uploadSessionManager: UploadSessionManager = koinGet()

    // Clean up expired upload sessions every 15 minutes
    CronScheduler.schedule(
        scope = scope,
        cronExpression = "*/15 * * * *", // Every 15 minutes
        taskName = "cleanup-expired-uploads",
    ) {
        val cleaned = uploadSessionManager.cleanupExpiredSessions(Duration.parse("24h"))
        if (cleaned > 0) {
            logger.info { "Cleaned up $cleaned expired upload sessions" }
        }
    }

    logger.info { "Background jobs configured" }
}

/**
 * Configures CORS based on server configuration.
 */
private fun Application.configureCors(serverConfig: HttpServerConfig) {
    install(CORS) {
        // Configure allowed origins
        if (serverConfig.corsAllowedOrigins.contains("*")) {
            anyHost()
        } else {
            serverConfig.corsAllowedOrigins.forEach { origin ->
                allowHost(origin.removePrefix("http://").removePrefix("https://"))
            }
        }

        // Allowed headers
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.Origin)
        allowHeader(HttpHeaders.AccessControlRequestMethod)
        allowHeader(HttpHeaders.AccessControlRequestHeaders)

        // Allowed methods
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Options)

        // Allow credentials (cookies, authorization headers)
        allowCredentials = true

        // Max age for preflight cache
        maxAgeInSeconds = 3600
    }
}
