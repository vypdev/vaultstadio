/**
 * VaultStadio Federation Service Tests
 */

package com.vaultstadio.core.domain.service

import arrow.core.right
import com.vaultstadio.core.domain.model.FederatedActivity
import com.vaultstadio.core.domain.model.FederatedIdentity
import com.vaultstadio.core.domain.model.FederatedInstance
import com.vaultstadio.core.domain.model.FederatedShare
import com.vaultstadio.core.domain.model.FederatedShareStatus
import com.vaultstadio.core.domain.model.FederationCapability
import com.vaultstadio.core.domain.model.FederationRequest
import com.vaultstadio.core.domain.model.InstanceStatus
import com.vaultstadio.core.domain.model.SharePermission
import com.vaultstadio.core.domain.repository.FederationRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FederationServiceTest {

    private lateinit var federationRepository: FederationRepository
    private lateinit var service: FederationService

    private val instanceConfig = InstanceConfig(
        domain = "local.example.com",
        name = "Local Instance",
        version = "2.0.0",
        publicKey = "local-public-key",
        privateKey = "local-private-key",
        capabilities = listOf(
            FederationCapability.SEND_SHARES,
            FederationCapability.RECEIVE_SHARES,
            FederationCapability.FEDERATED_IDENTITY,
        ),
    )

    @BeforeEach
    fun setup() {
        federationRepository = mockk()
        service = FederationService(federationRepository, instanceConfig)
    }

    @Test
    fun `requestFederation should create pending instance`() = runTest {
        val targetDomain = "remote.example.com"

        coEvery { federationRepository.findInstanceByDomain(targetDomain) } returns null.right()
        coEvery { federationRepository.registerInstance(any()) } answers {
            firstArg<FederatedInstance>().right()
        }

        val result = service.requestFederation(targetDomain, "Please accept")

        assertTrue(result.isRight())
        result.onRight { instance ->
            assertEquals(targetDomain, instance.domain)
            assertEquals(InstanceStatus.PENDING, instance.status)
        }
    }

    @Test
    fun `requestFederation should fail if already federated`() = runTest {
        val targetDomain = "remote.example.com"
        val now = Clock.System.now()

        val existingInstance = FederatedInstance(
            id = "instance-1",
            domain = targetDomain,
            name = "Remote",
            version = "2.0.0",
            publicKey = "key",
            status = InstanceStatus.ONLINE,
            registeredAt = now,
        )

        coEvery { federationRepository.findInstanceByDomain(targetDomain) } returns existingInstance.right()

        val result = service.requestFederation(targetDomain)

        assertTrue(result.isLeft())
    }

    @Test
    fun `handleFederationRequest should accept and register instance`() = runTest {
        val request = FederationRequest(
            sourceInstance = "new.example.com",
            sourceName = "New Instance",
            sourceVersion = "2.0.0",
            publicKey = "new-public-key",
            capabilities = listOf(FederationCapability.SEND_SHARES),
            message = "Hello",
        )

        coEvery { federationRepository.findInstanceByDomain(request.sourceInstance) } returns null.right()
        coEvery { federationRepository.registerInstance(any()) } answers {
            firstArg<FederatedInstance>().right()
        }

        val result = service.handleFederationRequest(request)

        assertTrue(result.isRight())
        result.onRight { response ->
            assertTrue(response.accepted)
            assertEquals(instanceConfig.publicKey, response.publicKey)
        }
    }

    @Test
    fun `listInstances should return all instances`() = runTest {
        val now = Clock.System.now()
        val instances = listOf(
            FederatedInstance(
                id = "i1",
                domain = "instance1.com",
                name = "Instance 1",
                version = "2.0.0",
                publicKey = "key1",
                status = InstanceStatus.ONLINE,
                registeredAt = now,
            ),
            FederatedInstance(
                id = "i2",
                domain = "instance2.com",
                name = "Instance 2",
                version = "2.0.0",
                publicKey = "key2",
                status = InstanceStatus.OFFLINE,
                registeredAt = now,
            ),
        )

        coEvery { federationRepository.listInstances(null, null) } returns instances.right()

        val result = service.listInstances()

        assertTrue(result.isRight())
        result.onRight { list ->
            assertEquals(2, list.size)
        }
    }

    @Test
    fun `createShare should create federated share`() = runTest {
        val userId = "user-1"
        val now = Clock.System.now()

        val targetInstance = FederatedInstance(
            id = "target-id",
            domain = "remote.example.com",
            name = "Remote",
            version = "2.0.0",
            publicKey = "key",
            capabilities = listOf(FederationCapability.RECEIVE_SHARES),
            status = InstanceStatus.ONLINE,
            registeredAt = now,
        )

        val input = CreateFederatedShareInput(
            itemId = "item-1",
            targetInstance = "remote.example.com",
            targetUserId = "remote-user",
            permissions = listOf(SharePermission.READ),
            expiresInDays = 7,
        )

        coEvery { federationRepository.findInstanceByDomain(input.targetInstance) } returns targetInstance.right()
        coEvery { federationRepository.createShare(any()) } answers {
            firstArg<FederatedShare>().right()
        }
        coEvery { federationRepository.recordActivity(any()) } answers {
            firstArg<FederatedActivity>().right()
        }

        val result = service.createShare(input, userId)

        assertTrue(result.isRight())
        result.onRight { share ->
            assertEquals("item-1", share.itemId)
            assertEquals("remote.example.com", share.targetInstance)
            assertEquals(FederatedShareStatus.PENDING, share.status)
        }
    }

    @Test
    fun `createShare should fail if instance not federated`() = runTest {
        val input = CreateFederatedShareInput(
            itemId = "item-1",
            targetInstance = "unknown.example.com",
            permissions = listOf(SharePermission.READ),
        )

        coEvery { federationRepository.findInstanceByDomain(input.targetInstance) } returns null.right()

        val result = service.createShare(input, "user-1")

        assertTrue(result.isLeft())
    }

    @Test
    fun `acceptShare should update share status`() = runTest {
        val shareId = "share-1"
        val now = Clock.System.now()

        val pendingShare = FederatedShare(
            id = shareId,
            itemId = "item-1",
            sourceInstance = "source.example.com",
            targetInstance = instanceConfig.domain,
            permissions = listOf(SharePermission.READ),
            createdBy = "user-1",
            createdAt = now,
            status = FederatedShareStatus.PENDING,
        )

        val acceptedShare = pendingShare.copy(
            status = FederatedShareStatus.ACCEPTED,
            acceptedAt = now,
        )

        coEvery { federationRepository.findShare(shareId) } returns pendingShare.right()
        coEvery { federationRepository.updateShareStatus(shareId, FederatedShareStatus.ACCEPTED, any()) } returns
            acceptedShare.right()

        val result = service.acceptShare(shareId)

        assertTrue(result.isRight())
        result.onRight { share ->
            assertEquals(FederatedShareStatus.ACCEPTED, share.status)
        }
    }

    @Test
    fun `linkIdentity should create federated identity`() = runTest {
        val now = Clock.System.now()

        val targetInstance = FederatedInstance(
            id = "target-id",
            domain = "remote.example.com",
            name = "Remote",
            version = "2.0.0",
            publicKey = "key",
            capabilities = listOf(FederationCapability.FEDERATED_IDENTITY),
            status = InstanceStatus.ONLINE,
            registeredAt = now,
        )

        coEvery { federationRepository.findInstanceByDomain("remote.example.com") } returns targetInstance.right()
        coEvery { federationRepository.linkIdentity(any()) } answers {
            firstArg<FederatedIdentity>().right()
        }

        val result = service.linkIdentity(
            localUserId = "local-user",
            remoteUserId = "remote-user",
            remoteInstance = "remote.example.com",
            displayName = "Remote User",
        )

        assertTrue(result.isRight())
        result.onRight { identity ->
            assertEquals("local-user", identity.localUserId)
            assertEquals("remote-user@remote.example.com", identity.federatedId)
        }
    }

    @Test
    fun `signMessage should create signed message`() {
        val payload = """{"type":"test"}"""

        val message = service.signMessage(payload)

        assertEquals(payload, message.payload)
        assertEquals(instanceConfig.domain, message.senderDomain)
        assertTrue(message.signature.isNotEmpty())
        assertTrue(message.nonce.isNotEmpty())
    }
}
