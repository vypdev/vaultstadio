/**
 * VaultStadio Federation Service Tests
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
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
import com.vaultstadio.core.domain.model.SignedFederationMessage
import com.vaultstadio.core.domain.repository.FederationRepository
import com.vaultstadio.core.exception.AuthorizationException
import com.vaultstadio.core.exception.DatabaseException
import com.vaultstadio.core.exception.InvalidOperationException
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun `handleFederationRequest should return not accepted when already federated`() = runTest {
        val now = Clock.System.now()
        val existingInstance = FederatedInstance(
            id = "existing-id",
            domain = "existing.example.com",
            name = "Existing",
            version = "2.0.0",
            publicKey = "key",
            status = InstanceStatus.ONLINE,
            registeredAt = now,
        )
        val request = FederationRequest(
            sourceInstance = "existing.example.com",
            sourceName = "Existing",
            sourceVersion = "2.0.0",
            publicKey = "key",
            capabilities = emptyList(),
            message = null,
        )
        coEvery { federationRepository.findInstanceByDomain(request.sourceInstance) } returns existingInstance.right()

        val result = service.handleFederationRequest(request)

        assertTrue(result.isRight())
        result.onRight { response ->
            assertFalse(response.accepted)
            assertTrue(response.message?.contains("Already federated") == true)
        }
    }

    @Test
    fun `getInstance should return instance when found`() = runTest {
        val now = Clock.System.now()
        val instance = FederatedInstance(
            id = "i1",
            domain = "found.example.com",
            name = "Found",
            version = "2.0.0",
            publicKey = "key",
            status = InstanceStatus.ONLINE,
            registeredAt = now,
        )
        coEvery { federationRepository.findInstanceByDomain("found.example.com") } returns instance.right()

        val result = service.getInstance("found.example.com")

        assertTrue(result.isRight())
        result.onRight { inst -> assertEquals("found.example.com", inst.domain) }
    }

    @Test
    fun `getInstance should return ItemNotFoundException when instance not found`() = runTest {
        coEvery { federationRepository.findInstanceByDomain("missing.example.com") } returns null.right()

        val result = service.getInstance("missing.example.com")

        assertTrue(result.isLeft())
        result.onLeft { err ->
            assertTrue(err is ItemNotFoundException)
            assertTrue(err.message.contains("Instance not found"))
        }
    }

    @Test
    fun `blockInstance should update instance status to BLOCKED`() = runTest {
        val now = Clock.System.now()
        val blocked = FederatedInstance(
            id = "inst-1",
            domain = "block.example.com",
            name = "Blocked",
            version = "2.0.0",
            publicKey = "key",
            status = InstanceStatus.BLOCKED,
            registeredAt = now,
        )
        coEvery { federationRepository.updateInstanceStatus("inst-1", InstanceStatus.BLOCKED, null) } returns blocked.right()

        val result = service.blockInstance("inst-1")

        assertTrue(result.isRight())
        result.onRight { inst -> assertEquals(InstanceStatus.BLOCKED, inst.status) }
    }

    @Test
    fun `removeInstance should call repository`() = runTest {
        coEvery { federationRepository.removeInstance("inst-1") } returns Unit.right()

        val result = service.removeInstance("inst-1")

        assertTrue(result.isRight())
    }

    @Test
    fun `updateInstanceHealth should return not found when instance missing`() = runTest {
        coEvery { federationRepository.findInstanceByDomain("missing.com") } returns null.right()

        val result = service.updateInstanceHealth("missing.com", isOnline = true)

        assertTrue(result.isLeft())
        result.onLeft { err -> assertTrue(err is ItemNotFoundException) }
    }

    @Test
    fun `updateInstanceHealth should return instance without update when status is BLOCKED`() = runTest {
        val now = Clock.System.now()
        val blocked = FederatedInstance(
            id = "b1",
            domain = "blocked.com",
            name = "Blocked",
            version = "2.0.0",
            publicKey = "key",
            status = InstanceStatus.BLOCKED,
            registeredAt = now,
        )
        coEvery { federationRepository.findInstanceByDomain("blocked.com") } returns blocked.right()

        val result = service.updateInstanceHealth("blocked.com", isOnline = true)

        assertTrue(result.isRight())
        result.onRight { inst -> assertEquals(InstanceStatus.BLOCKED, inst.status) }
    }

    @Test
    fun `updateInstanceHealth should update status to ONLINE when isOnline true`() = runTest {
        val now = Clock.System.now()
        val instance = FederatedInstance(
            id = "i1",
            domain = "online.com",
            name = "Online",
            version = "2.0.0",
            publicKey = "key",
            status = InstanceStatus.OFFLINE,
            registeredAt = now,
        )
        val updated = instance.copy(status = InstanceStatus.ONLINE, lastSeenAt = now)
        coEvery { federationRepository.findInstanceByDomain("online.com") } returns instance.right()
        coEvery { federationRepository.updateInstanceStatus("i1", InstanceStatus.ONLINE, any()) } returns updated.right()

        val result = service.updateInstanceHealth("online.com", isOnline = true)

        assertTrue(result.isRight())
        result.onRight { inst -> assertEquals(InstanceStatus.ONLINE, inst.status) }
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
        result.onLeft { err -> assertTrue(err is ItemNotFoundException) }
    }

    @Test
    fun `createShare should fail if instance is not online`() = runTest {
        val now = Clock.System.now()
        val offlineInstance = FederatedInstance(
            id = "target-id",
            domain = "remote.example.com",
            name = "Remote",
            version = "2.0.0",
            publicKey = "key",
            capabilities = listOf(FederationCapability.RECEIVE_SHARES),
            status = InstanceStatus.OFFLINE,
            registeredAt = now,
        )
        val input = CreateFederatedShareInput(
            itemId = "item-1",
            targetInstance = "remote.example.com",
            permissions = listOf(SharePermission.READ),
        )
        coEvery { federationRepository.findInstanceByDomain(input.targetInstance) } returns offlineInstance.right()

        val result = service.createShare(input, "user-1")

        assertTrue(result.isLeft())
        result.onLeft { err ->
            assertTrue(err is InvalidOperationException)
            assertTrue(err.message.contains("not online"))
        }
    }

    @Test
    fun `createShare should fail if instance does not support RECEIVE_SHARES`() = runTest {
        val now = Clock.System.now()
        val instanceNoReceive = FederatedInstance(
            id = "target-id",
            domain = "remote.example.com",
            name = "Remote",
            version = "2.0.0",
            publicKey = "key",
            capabilities = listOf(FederationCapability.SEND_SHARES),
            status = InstanceStatus.ONLINE,
            registeredAt = now,
        )
        val input = CreateFederatedShareInput(
            itemId = "item-1",
            targetInstance = "remote.example.com",
            permissions = listOf(SharePermission.READ),
        )
        coEvery { federationRepository.findInstanceByDomain(input.targetInstance) } returns instanceNoReceive.right()

        val result = service.createShare(input, "user-1")

        assertTrue(result.isLeft())
        result.onLeft { err ->
            assertTrue(err is InvalidOperationException)
            assertTrue(err.message.contains("does not support receiving shares"))
        }
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
    fun `acceptShare should return ItemNotFoundException when share not found`() = runTest {
        coEvery { federationRepository.findShare("missing-share") } returns null.right()

        val result = service.acceptShare("missing-share")

        assertTrue(result.isLeft())
        result.onLeft { err ->
            assertTrue(err is ItemNotFoundException)
            assertTrue(err.message.contains("Share not found"))
        }
    }

    @Test
    fun `acceptShare should return InvalidOperationException when share not pending`() = runTest {
        val now = Clock.System.now()
        val acceptedShare = FederatedShare(
            id = "share-1",
            itemId = "item-1",
            sourceInstance = "source.example.com",
            targetInstance = instanceConfig.domain,
            permissions = listOf(SharePermission.READ),
            createdBy = "user-1",
            createdAt = now,
            status = FederatedShareStatus.ACCEPTED,
            acceptedAt = now,
        )
        coEvery { federationRepository.findShare("share-1") } returns acceptedShare.right()

        val result = service.acceptShare("share-1")

        assertTrue(result.isLeft())
        result.onLeft { err ->
            assertTrue(err is InvalidOperationException)
            assertTrue(err.message.contains("not pending"))
        }
    }

    @Test
    fun `declineShare should return ItemNotFoundException when share not found`() = runTest {
        coEvery { federationRepository.findShare("missing-share") } returns null.right()

        val result = service.declineShare("missing-share")

        assertTrue(result.isLeft())
        result.onLeft { err -> assertTrue(err is ItemNotFoundException) }
    }

    @Test
    fun `declineShare should update share status when pending`() = runTest {
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
        val declinedShare = pendingShare.copy(status = FederatedShareStatus.DECLINED)
        coEvery { federationRepository.findShare(shareId) } returns pendingShare.right()
        coEvery { federationRepository.updateShareStatus(shareId, FederatedShareStatus.DECLINED, any()) } returns
            declinedShare.right()

        val result = service.declineShare(shareId)

        assertTrue(result.isRight())
        result.onRight { share -> assertEquals(FederatedShareStatus.DECLINED, share.status) }
    }

    @Test
    fun `revokeShare should return ItemNotFoundException when share not found`() = runTest {
        coEvery { federationRepository.findShare("missing-share") } returns null.right()

        val result = service.revokeShare("missing-share", "user-1")

        assertTrue(result.isLeft())
        result.onLeft { err -> assertTrue(err is ItemNotFoundException) }
    }

    @Test
    fun `revokeShare should return AuthorizationException when user is not creator`() = runTest {
        val now = Clock.System.now()
        val share = FederatedShare(
            id = "share-1",
            itemId = "item-1",
            sourceInstance = "source.com",
            targetInstance = instanceConfig.domain,
            permissions = listOf(SharePermission.READ),
            createdBy = "creator-user",
            createdAt = now,
            status = FederatedShareStatus.ACCEPTED,
        )
        coEvery { federationRepository.findShare("share-1") } returns share.right()

        val result = service.revokeShare("share-1", "other-user")

        assertTrue(result.isLeft())
        result.onLeft { err ->
            assertTrue(err is AuthorizationException)
            assertTrue(err.message.contains("Not authorized"))
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

    @Nested
    inner class VerifyMessageTests {

        @Test
        fun `verifyMessage returns Left when instance lookup fails`() = runTest {
            val message = SignedFederationMessage(
                payload = "test",
                signature = "sig",
                timestamp = Clock.System.now(),
                nonce = "n1",
                senderDomain = "remote.example.com",
                algorithm = "Ed25519",
            )
            coEvery { federationRepository.findInstanceByDomain("remote.example.com") } returns
                DatabaseException("DB error").left()

            val result = service.verifyMessage(message)

            assertTrue(result.isLeft())
        }

        @Test
        fun `verifyMessage returns Right false when instance has blank public key`() = runTest {
            val message = SignedFederationMessage(
                payload = "test",
                signature = "sig",
                timestamp = Clock.System.now(),
                nonce = "n1",
                senderDomain = "remote.example.com",
                algorithm = "Ed25519",
            )
            val instance = FederatedInstance(
                id = "i1",
                domain = "remote.example.com",
                name = "Remote",
                version = "2.0.0",
                publicKey = "",
                status = InstanceStatus.ONLINE,
                registeredAt = Clock.System.now(),
            )
            coEvery { federationRepository.findInstanceByDomain("remote.example.com") } returns instance.right()

            val result = service.verifyMessage(message)

            assertTrue(result.isRight())
            result.onRight { valid -> assertFalse(valid) }
        }

        @Test
        fun `verifyMessage returns Right true when signature is valid`() = runTest {
            val remoteCrypto = FederationCryptoService()
            val publicKey = remoteCrypto.getPublicKeyBase64()!!
            val now = Clock.System.now()
            val payload = "hello"
            val nonce = "nonce-1"
            val signedPayload = remoteCrypto.signFederationMessage(payload, nonce, now.epochSeconds)!!
            val message = SignedFederationMessage(
                payload = payload,
                signature = signedPayload.signature,
                timestamp = now,
                nonce = nonce,
                senderDomain = "remote.example.com",
                algorithm = signedPayload.algorithm,
            )
            val instance = FederatedInstance(
                id = "i1",
                domain = "remote.example.com",
                name = "Remote",
                version = "2.0.0",
                publicKey = publicKey,
                status = InstanceStatus.ONLINE,
                registeredAt = Clock.System.now(),
            )
            coEvery { federationRepository.findInstanceByDomain("remote.example.com") } returns instance.right()

            val result = service.verifyMessage(message)

            assertTrue(result.isRight())
            result.onRight { valid -> assertTrue(valid) }
        }
    }

    @Nested
    inner class RunHealthChecksTests {

        @Test
        fun `runHealthChecks returns Left when listInstances fails`() = runTest {
            coEvery { federationRepository.listInstances(any(), any()) } returns
                DatabaseException("DB error").left()

            val result = service.runHealthChecks()

            assertTrue(result.isLeft())
        }

        @Test
        fun `runHealthChecks returns Right empty map when no instances`() = runTest {
            coEvery { federationRepository.listInstances(any(), any()) } returns emptyList<FederatedInstance>().right()

            val result = service.runHealthChecks()

            assertTrue(result.isRight())
            result.onRight { map -> assertTrue(map.isEmpty()) }
        }
    }

    @Nested
    inner class CleanupTests {

        @Test
        fun `cleanup returns Left when getExpiredShares fails`() = runTest {
            coEvery { federationRepository.getExpiredShares(any()) } returns
                DatabaseException("DB error").left()

            val result = service.cleanup(30)

            assertTrue(result.isLeft())
        }

        @Test
        fun `cleanup returns Right count when expired shares and prune succeed`() = runTest {
            val now = Clock.System.now()
            val expiredShare = FederatedShare(
                id = "share-1",
                itemId = "item-1",
                sourceInstance = "source.com",
                targetInstance = instanceConfig.domain,
                permissions = listOf(SharePermission.READ),
                createdBy = "u1",
                createdAt = now,
                status = FederatedShareStatus.PENDING,
            )
            coEvery { federationRepository.getExpiredShares(any()) } returns listOf(expiredShare).right()
            coEvery { federationRepository.updateShareStatus(any(), any(), any()) } returns expiredShare.copy(
                status = FederatedShareStatus.EXPIRED,
            ).right()
            coEvery { federationRepository.pruneActivities(any()) } returns 3.right()

            val result = service.cleanup(30)

            assertTrue(result.isRight())
            result.onRight { count -> assertEquals(4, count) }
        }
    }
}
