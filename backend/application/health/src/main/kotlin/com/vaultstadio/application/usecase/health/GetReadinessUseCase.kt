/**
 * Get Readiness Use Case
 *
 * Application use case for readiness check (database + storage).
 */

package com.vaultstadio.application.usecase.health

import com.vaultstadio.domain.auth.repository.UserRepository
import com.vaultstadio.core.domain.service.StorageBackend
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

/**
 * Use case for readiness check (database and storage only).
 */
interface GetReadinessUseCase {

    suspend operator fun invoke(): ReadinessResult
}

/**
 * Default implementation using [UserRepository] and [StorageBackend].
 */
class GetReadinessUseCaseImpl(
    private val userRepository: UserRepository,
    private val storageBackend: StorageBackend,
) : GetReadinessUseCase {

    override suspend fun invoke(): ReadinessResult = withContext(Dispatchers.Default) {
        val checks = mutableMapOf<String, ComponentHealthResult>()
        checks["database"] = checkDatabase(userRepository)
        checks["storage"] = checkStorage(storageBackend)
        val ready = checks.values.all { it.status == "healthy" }
        ReadinessResult(ready = ready, checks = checks)
    }

    private suspend fun checkDatabase(userRepository: UserRepository): ComponentHealthResult {
        return try {
            val start = System.currentTimeMillis()
            val result = userRepository.countAll()
            val latency = System.currentTimeMillis() - start
            result.fold(
                { error ->
                    logger.warn { "Database health check failed: ${error.message}" }
                    ComponentHealthResult("unhealthy", error.message, latency)
                },
                { count ->
                    ComponentHealthResult("healthy", "Connected, $count users", latency)
                },
            )
        } catch (e: Exception) {
            logger.error(e) { "Database health check exception" }
            ComponentHealthResult("unhealthy", e.message ?: "Unknown error")
        }
    }

    private suspend fun checkStorage(storageBackend: StorageBackend): ComponentHealthResult {
        return try {
            val start = System.currentTimeMillis()
            val available = storageBackend.isAvailable()
            val latency = System.currentTimeMillis() - start
            available.fold(
                { error ->
                    logger.warn { "Storage health check failed: ${error.message}" }
                    ComponentHealthResult("unhealthy", error.message, latency)
                },
                { isAvailable ->
                    if (isAvailable) {
                        ComponentHealthResult("healthy", "Storage backend available", latency)
                    } else {
                        ComponentHealthResult("unhealthy", "Storage backend not available", latency)
                    }
                },
            )
        } catch (e: Exception) {
            logger.error(e) { "Storage health check exception" }
            ComponentHealthResult("unhealthy", e.message ?: "Unknown error")
        }
    }
}
