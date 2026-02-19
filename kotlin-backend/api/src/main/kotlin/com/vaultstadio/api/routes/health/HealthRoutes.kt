/**
 * VaultStadio Health Check Routes
 *
 * Provides health and readiness endpoints for container orchestration.
 */

package com.vaultstadio.api.routes.health

import com.vaultstadio.api.application.usecase.health.ComponentHealthResult
import com.vaultstadio.api.application.usecase.health.GetDetailedHealthUseCase
import com.vaultstadio.api.application.usecase.health.GetReadinessUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.get as koinGet

private fun ComponentHealthResult.toResponse() = ComponentHealth(
    status = status,
    message = message,
    latencyMs = latencyMs,
)

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
        val getReadinessUseCase: GetReadinessUseCase = call.application.koinGet()
        val result = getReadinessUseCase()
        val response = ReadinessResponse(
            ready = result.ready,
            checks = result.checks.mapValues { it.value.toResponse() },
        )
        val statusCode = if (result.ready) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
        call.respond(statusCode, response)
    }

    /**
     * Detailed health check with all component statuses.
     */
    get("/health/detailed") {
        val getDetailedHealthUseCase: GetDetailedHealthUseCase = call.application.koinGet()
        val uptimeMs = System.currentTimeMillis() - startTime
        val result = getDetailedHealthUseCase(uptimeMs)
        val response = ReadinessResponse(
            ready = result.ready,
            checks = result.checks.mapValues { it.value.toResponse() },
        )
        val statusCode = if (result.ready) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
        call.respond(statusCode, response)
    }
}
