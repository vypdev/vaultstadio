/**
 * Federation maintenance helpers: health checks and cleanup.
 * Extracted from FederationService to keep the main file under the line limit.
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.core.domain.model.FederatedInstance
import com.vaultstadio.core.domain.model.FederatedShareStatus
import com.vaultstadio.core.domain.model.InstanceStatus
import com.vaultstadio.core.domain.repository.FederationRepository
import com.vaultstadio.domain.common.exception.StorageException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

private val healthCheckClient by lazy {
    HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 10_000
        }
    }
}

/**
 * Check if a federated instance is online by making an HTTP request.
 */
internal suspend fun checkInstanceHealth(domain: String): Boolean {
    return try {
        withTimeoutOrNull(10.seconds) {
            val healthUrl = "https://$domain/api/v1/health"
            val response = healthCheckClient.get(healthUrl)
            response.status.isSuccess()
        } ?: false
    } catch (e: Exception) {
        logger.debug { "Health check failed for $domain: ${e.message}" }
        false
    }
}

/**
 * Run health checks on all federated instances.
 */
internal suspend fun runFederationHealthChecks(
    federationRepository: FederationRepository,
    updateInstanceHealth: suspend (String, Boolean) -> Either<StorageException, FederatedInstance>,
): Either<StorageException, Map<String, Boolean>> {
    val instancesResult = federationRepository.listInstances()
    return when (instancesResult) {
        is Either.Left -> instancesResult
        is Either.Right -> {
            val instances = instancesResult.value
            val results = mutableMapOf<String, Boolean>()
            for (instance in instances.filter { i -> i.status != InstanceStatus.BLOCKED }) {
                val isOnline = checkInstanceHealth(instance.domain)
                results[instance.domain] = isOnline
                updateInstanceHealth(instance.domain, isOnline)
                logger.debug { "Health check for ${instance.domain}: ${if (isOnline) "online" else "offline"}" }
            }
            results.right()
        }
    }
}

/**
 * Clean up expired shares and old activities.
 */
internal suspend fun cleanupFederation(
    federationRepository: FederationRepository,
    olderThanDays: Int = 30,
): Either<StorageException, Int> {
    val threshold = Clock.System.now().minus(olderThanDays.days)

    val expiredResult = federationRepository.getExpiredShares(Clock.System.now())
    return when (expiredResult) {
        is Either.Left -> expiredResult
        is Either.Right -> {
            var count = 0
            for (share in expiredResult.value) {
                federationRepository.updateShareStatus(share.id, FederatedShareStatus.EXPIRED)
                count++
            }
            when (val pruneResult = federationRepository.pruneActivities(threshold)) {
                is Either.Left -> pruneResult
                is Either.Right -> (count + pruneResult.value).right()
            }
        }
    }
}
