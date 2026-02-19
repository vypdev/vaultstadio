/**
 * Result types for health check use cases.
 */

package com.vaultstadio.api.application.usecase.health

/**
 * Result of a single component health check.
 */
data class ComponentHealthResult(
    val status: String,
    val message: String? = null,
    val latencyMs: Long? = null,
)

/**
 * Aggregated readiness/detailed health result.
 */
data class ReadinessResult(
    val ready: Boolean,
    val checks: Map<String, ComponentHealthResult>,
)
