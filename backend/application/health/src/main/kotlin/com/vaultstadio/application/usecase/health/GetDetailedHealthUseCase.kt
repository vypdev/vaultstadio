/**
 * Get Detailed Health Use Case
 *
 * Application use case for detailed health (readiness + memory + uptime).
 */

package com.vaultstadio.application.usecase.health

/**
 * Use case for detailed health check (readiness checks + memory + uptime).
 */
interface GetDetailedHealthUseCase {

    suspend operator fun invoke(uptimeMs: Long): ReadinessResult
}

/**
 * Default implementation: delegates to [GetReadinessUseCase] and adds memory + uptime.
 */
class GetDetailedHealthUseCaseImpl(
    private val getReadinessUseCase: GetReadinessUseCase,
) : GetDetailedHealthUseCase {

    override suspend fun invoke(uptimeMs: Long): ReadinessResult {
        val base = getReadinessUseCase()
        val checks = base.checks.toMutableMap()
        checks["memory"] = checkMemory()
        checks["uptime"] = ComponentHealthResult(
            status = "healthy",
            message = "Running for ${uptimeMs / 1000}s",
        )
        val ready = checks.values.all { it.status == "healthy" }
        return ReadinessResult(ready = ready, checks = checks)
    }

    private fun checkMemory(): ComponentHealthResult {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val usedPercentage = (usedMemory.toDouble() / maxMemory * 100).toInt()
        val status = when {
            usedPercentage > 90 -> "unhealthy"
            usedPercentage > 75 -> "degraded"
            else -> "healthy"
        }
        return ComponentHealthResult(
            status = status,
            message = "Used ${usedMemory / 1024 / 1024}MB of ${maxMemory / 1024 / 1024}MB ($usedPercentage%)",
        )
    }
}
