/**
 * GetDetailedHealthUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.health

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetDetailedHealthUseCaseTest {

    private val getReadinessUseCase: GetReadinessUseCase = mockk()
    private val useCase = GetDetailedHealthUseCaseImpl(getReadinessUseCase)

    @Test
    fun invokeReturnsResultWithMemoryAndUptimeWhenReadinessHealthy() = runTest {
        val base = ReadinessResult(
            ready = true,
            checks = mapOf(
                "database" to ComponentHealthResult("healthy", "OK"),
                "storage" to ComponentHealthResult("healthy", "OK"),
            ),
        )
        coEvery { getReadinessUseCase() } returns base

        val result = useCase(uptimeMs = 120_000)

        assertTrue(result.checks.containsKey("memory"))
        assertTrue(result.checks.containsKey("uptime"))
        assertEquals("healthy", result.checks["uptime"]?.status)
        assertTrue(result.checks["uptime"]!!.message!!.contains("120"))
    }

    @Test
    fun invokeReturnsNotReadyWhenReadinessUnhealthy() = runTest {
        val base = ReadinessResult(
            ready = false,
            checks = mapOf(
                "database" to ComponentHealthResult("unhealthy", "Down"),
                "storage" to ComponentHealthResult("healthy", "OK"),
            ),
        )
        coEvery { getReadinessUseCase() } returns base

        val result = useCase(uptimeMs = 0)

        assertFalse(result.ready)
        assertEquals("unhealthy", result.checks["database"]?.status)
    }
}
