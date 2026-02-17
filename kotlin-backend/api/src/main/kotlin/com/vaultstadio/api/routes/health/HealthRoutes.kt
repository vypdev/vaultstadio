/**
 * VaultStadio Health Check Routes
 *
 * Provides health and readiness endpoints for container orchestration.
 */

package com.vaultstadio.api.routes.health

import com.vaultstadio.core.domain.repository.UserRepository
import com.vaultstadio.core.domain.service.StorageBackend
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.get as koinGet

private val logger = KotlinLogging.logger {}

/**
 * Basic health response.
 */
@Serializable
data class HealthResponse(
    val status: String,
    val version: String,
    val uptime: Long,
)

/**
 * Detailed readiness response with component status.
 */
@Serializable
data class ReadinessResponse(
    val ready: Boolean,
    val checks: Map<String, ComponentHealth>,
)

/**
 * Individual component health status.
 */
@Serializable
data class ComponentHealth(
    val status: String,
    val message: String? = null,
    val latencyMs: Long? = null,
)

private val startTime = System.currentTimeMillis()
private const val VERSION = "1.0.0"

/**
 * Configures health check routes.
 */
fun Route.healthRoutes() {
    /**
     * Basic health check - always returns 200 if the server is running.
     * Used by load balancers for basic liveness checks.
     */
    get("/health") {
        call.respond(
            HttpStatusCode.OK,
            HealthResponse(
                status = "healthy",
                version = VERSION,
                uptime = System.currentTimeMillis() - startTime,
            ),
        )
    }

    /**
     * Readiness check - verifies all dependencies are available.
     * Used by Kubernetes/orchestrators before routing traffic.
     */
    get("/ready") {
        val userRepository: UserRepository = call.application.koinGet()
        val storageBackend: StorageBackend = call.application.koinGet()

        val checks = mutableMapOf<String, ComponentHealth>()
        var allHealthy = true

        // Check database connection
        val dbHealth = checkDatabase(userRepository)
        checks["database"] = dbHealth
        if (dbHealth.status != "healthy") allHealthy = false

        // Check storage backend
        val storageHealth = checkStorage(storageBackend)
        checks["storage"] = storageHealth
        if (storageHealth.status != "healthy") allHealthy = false

        val response = ReadinessResponse(
            ready = allHealthy,
            checks = checks,
        )

        val statusCode = if (allHealthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
        call.respond(statusCode, response)
    }

    /**
     * Detailed health check with all component statuses.
     */
    get("/health/detailed") {
        val userRepository: UserRepository = call.application.koinGet()
        val storageBackend: StorageBackend = call.application.koinGet()

        val checks = mutableMapOf<String, ComponentHealth>()

        // Database check
        checks["database"] = checkDatabase(userRepository)

        // Storage check
        checks["storage"] = checkStorage(storageBackend)

        // Memory check
        checks["memory"] = checkMemory()

        // Uptime
        checks["uptime"] = ComponentHealth(
            status = "healthy",
            message = "Running for ${(System.currentTimeMillis() - startTime) / 1000}s",
        )

        val allHealthy = checks.values.all { it.status == "healthy" }

        call.respond(
            if (allHealthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable,
            ReadinessResponse(ready = allHealthy, checks = checks),
        )
    }
}

/**
 * Checks database connectivity by executing a simple query.
 */
private suspend fun checkDatabase(userRepository: UserRepository): ComponentHealth {
    return try {
        val startTime = System.currentTimeMillis()
        val result = userRepository.countAll()
        val latency = System.currentTimeMillis() - startTime

        result.fold(
            { error ->
                logger.warn { "Database health check failed: ${error.message}" }
                ComponentHealth(
                    status = "unhealthy",
                    message = error.message,
                    latencyMs = latency,
                )
            },
            { count ->
                ComponentHealth(
                    status = "healthy",
                    message = "Connected, $count users",
                    latencyMs = latency,
                )
            },
        )
    } catch (e: Exception) {
        logger.error(e) { "Database health check exception" }
        ComponentHealth(
            status = "unhealthy",
            message = e.message ?: "Unknown error",
        )
    }
}

/**
 * Checks storage backend availability.
 */
private suspend fun checkStorage(storageBackend: StorageBackend): ComponentHealth {
    return try {
        val startTime = System.currentTimeMillis()
        val available = storageBackend.isAvailable()
        val latency = System.currentTimeMillis() - startTime

        available.fold(
            { error ->
                logger.warn { "Storage health check failed: ${error.message}" }
                ComponentHealth(
                    status = "unhealthy",
                    message = error.message,
                    latencyMs = latency,
                )
            },
            { isAvailable ->
                if (isAvailable) {
                    ComponentHealth(
                        status = "healthy",
                        message = "Storage backend available",
                        latencyMs = latency,
                    )
                } else {
                    ComponentHealth(
                        status = "unhealthy",
                        message = "Storage backend not available",
                        latencyMs = latency,
                    )
                }
            },
        )
    } catch (e: Exception) {
        logger.error(e) { "Storage health check exception" }
        ComponentHealth(
            status = "unhealthy",
            message = e.message ?: "Unknown error",
        )
    }
}

/**
 * Checks JVM memory status.
 */
private fun checkMemory(): ComponentHealth {
    val runtime = Runtime.getRuntime()
    val maxMemory = runtime.maxMemory()
    val totalMemory = runtime.totalMemory()
    val freeMemory = runtime.freeMemory()
    val usedMemory = totalMemory - freeMemory
    val usedPercentage = (usedMemory.toDouble() / maxMemory * 100).toInt()

    val status = when {
        usedPercentage > 90 -> "unhealthy"
        usedPercentage > 75 -> "degraded"
        else -> "healthy"
    }

    return ComponentHealth(
        status = status,
        message = "Used ${usedMemory / 1024 / 1024}MB of ${maxMemory / 1024 / 1024}MB ($usedPercentage%)",
    )
}
