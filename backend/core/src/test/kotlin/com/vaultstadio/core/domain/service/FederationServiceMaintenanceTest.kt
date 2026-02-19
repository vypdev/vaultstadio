/**
 * VaultStadio Federation Maintenance Helpers Tests
 *
 * Unit tests for internal maintenance helpers: runFederationHealthChecks, cleanupFederation.
 * checkInstanceHealth is not unit-tested (performs real HTTP).
 */

package com.vaultstadio.core.domain.service

import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.FederatedInstance
import com.vaultstadio.core.domain.model.FederatedShare
import com.vaultstadio.core.domain.model.FederatedShareStatus
import com.vaultstadio.core.domain.model.SharePermission
import com.vaultstadio.core.domain.repository.FederationRepository
import com.vaultstadio.core.exception.DatabaseException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FederationServiceMaintenanceTest {

    @Test
    fun `runFederationHealthChecks returns Left when listInstances fails`() = runTest {
        val repo = mockk<FederationRepository>()
        coEvery { repo.listInstances(any(), any()) } returns DatabaseException("error").left()
        var updateCalled = false
        val result = runFederationHealthChecks(repo) { _: String, _: Boolean ->
            updateCalled = true
            throw UnsupportedOperationException("should not be called")
        }
        assertTrue(result.isLeft())
        assertTrue(!updateCalled)
    }

    @Test
    fun `runFederationHealthChecks returns Right empty map when no instances`() = runTest {
        val repo = mockk<FederationRepository>()
        coEvery { repo.listInstances(any(), any()) } returns emptyList<FederatedInstance>().right()
        val result =
            runFederationHealthChecks(repo) { _, _ -> throw UnsupportedOperationException("should not be called") }
        assertTrue(result.isRight())
        result.onRight { map -> assertTrue(map.isEmpty()) }
    }

    @Test
    fun `cleanupFederation returns Left when getExpiredShares fails`() = runTest {
        val repo = mockk<FederationRepository>()
        coEvery { repo.getExpiredShares(any()) } returns DatabaseException("error").left()
        val result = cleanupFederation(repo, 30)
        assertTrue(result.isLeft())
    }

    @Test
    fun `cleanupFederation returns Right count when expired shares and prune succeed`() = runTest {
        val repo = mockk<FederationRepository>()
        val now = Clock.System.now()
        val share = FederatedShare(
            id = "s1",
            itemId = "i1",
            sourceInstance = "src.com",
            targetInstance = "tgt.com",
            permissions = listOf(SharePermission.READ),
            createdBy = "u1",
            createdAt = now,
            status = FederatedShareStatus.PENDING,
        )
        coEvery { repo.getExpiredShares(any()) } returns listOf(share).right()
        coEvery { repo.updateShareStatus(any(), any(), any()) } returns share.copy(
            status = FederatedShareStatus.EXPIRED,
        ).right()
        coEvery { repo.pruneActivities(any()) } returns 2.right()
        val result = cleanupFederation(repo, 30)
        assertTrue(result.isRight())
        result.onRight { count -> assertEquals(3, count) }
    }
}
