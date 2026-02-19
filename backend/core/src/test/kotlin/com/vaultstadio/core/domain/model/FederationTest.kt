/**
 * VaultStadio Federation Model Tests
 */

package com.vaultstadio.core.domain.model

import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class FederationTest {

    @Test
    fun `FederatedInstance should have correct properties`() {
        val now = Clock.System.now()
        val instance = FederatedInstance(
            id = "instance-1",
            domain = "storage.example.com",
            name = "Example Storage",
            description = "A storage instance",
            version = "2.0.0",
            publicKey = "public-key-123",
            capabilities = listOf(
                FederationCapability.SEND_SHARES,
                FederationCapability.RECEIVE_SHARES,
            ),
            status = InstanceStatus.ONLINE,
            lastSeenAt = now,
            registeredAt = now,
        )

        assertEquals("instance-1", instance.id)
        assertEquals("storage.example.com", instance.domain)
        assertEquals("Example Storage", instance.name)
        assertEquals("2.0.0", instance.version)
        assertEquals(InstanceStatus.ONLINE, instance.status)
        assertTrue(instance.isOnline)
        assertEquals("vaultstadio://storage.example.com", instance.uri)
    }

    @Test
    fun `FederatedInstance should not be online when offline`() {
        val instance = FederatedInstance(
            id = "instance-1",
            domain = "offline.example.com",
            name = "Offline Instance",
            version = "1.0.0",
            publicKey = "key",
            status = InstanceStatus.OFFLINE,
            registeredAt = Clock.System.now(),
        )

        assertFalse(instance.isOnline)
    }

    @Test
    fun `InstanceStatus should have all status values`() {
        val statuses = InstanceStatus.entries

        assertTrue(statuses.contains(InstanceStatus.PENDING))
        assertTrue(statuses.contains(InstanceStatus.ONLINE))
        assertTrue(statuses.contains(InstanceStatus.OFFLINE))
        assertTrue(statuses.contains(InstanceStatus.BLOCKED))
        assertTrue(statuses.contains(InstanceStatus.REMOVED))
    }

    @Test
    fun `FederationCapability should have all capabilities`() {
        val capabilities = FederationCapability.entries

        assertTrue(capabilities.contains(FederationCapability.RECEIVE_SHARES))
        assertTrue(capabilities.contains(FederationCapability.SEND_SHARES))
        assertTrue(capabilities.contains(FederationCapability.FEDERATED_IDENTITY))
        assertTrue(capabilities.contains(FederationCapability.FEDERATED_SEARCH))
        assertTrue(capabilities.contains(FederationCapability.ACTIVITY_STREAM))
        assertTrue(capabilities.contains(FederationCapability.REAL_TIME_EVENTS))
    }

    @Test
    fun `FederatedShare should have correct properties`() {
        val now = Clock.System.now()
        val share = FederatedShare(
            id = "share-1",
            itemId = "item-1",
            sourceInstance = "source.example.com",
            targetInstance = "target.example.com",
            targetUserId = "remote-user-1",
            permissions = listOf(SharePermission.READ, SharePermission.WRITE),
            expiresAt = now + (7 * 24).hours,
            createdBy = "user-1",
            createdAt = now,
            status = FederatedShareStatus.PENDING,
        )

        assertEquals("share-1", share.id)
        assertEquals("source.example.com", share.sourceInstance)
        assertEquals("target.example.com", share.targetInstance)
        assertEquals(FederatedShareStatus.PENDING, share.status)
        assertEquals(2, share.permissions.size)
    }

    @Test
    fun `SharePermission should have all permission levels`() {
        val permissions = SharePermission.entries

        assertTrue(permissions.contains(SharePermission.READ))
        assertTrue(permissions.contains(SharePermission.WRITE))
        assertTrue(permissions.contains(SharePermission.DELETE))
        assertTrue(permissions.contains(SharePermission.SHARE))
        assertTrue(permissions.contains(SharePermission.ADMIN))
    }

    @Test
    fun `FederatedShareStatus should have all status values`() {
        val statuses = FederatedShareStatus.entries

        assertTrue(statuses.contains(FederatedShareStatus.PENDING))
        assertTrue(statuses.contains(FederatedShareStatus.ACCEPTED))
        assertTrue(statuses.contains(FederatedShareStatus.DECLINED))
        assertTrue(statuses.contains(FederatedShareStatus.REVOKED))
        assertTrue(statuses.contains(FederatedShareStatus.EXPIRED))
    }

    @Test
    fun `FederatedIdentity should have correct federated ID`() {
        val identity = FederatedIdentity(
            id = "identity-1",
            localUserId = "local-user-1",
            remoteUserId = "john.doe",
            remoteInstance = "remote.example.com",
            displayName = "John Doe",
            email = "john@example.com",
            verified = true,
            linkedAt = Clock.System.now(),
        )

        assertEquals("john.doe@remote.example.com", identity.federatedId)
        assertEquals("John Doe", identity.displayName)
        assertTrue(identity.verified)
    }

    @Test
    fun `FederatedActivity should have correct properties`() {
        val now = Clock.System.now()
        val activity = FederatedActivity(
            id = "activity-1",
            instanceDomain = "source.example.com",
            activityType = FederatedActivityType.SHARE_CREATED,
            actorId = "user@source.example.com",
            objectId = "share-1",
            objectType = "share",
            summary = "User shared a file",
            timestamp = now,
        )

        assertEquals("activity-1", activity.id)
        assertEquals(FederatedActivityType.SHARE_CREATED, activity.activityType)
        assertEquals("User shared a file", activity.summary)
    }

    @Test
    fun `FederatedActivityType should have all activity types`() {
        val types = FederatedActivityType.entries

        assertTrue(types.contains(FederatedActivityType.SHARE_CREATED))
        assertTrue(types.contains(FederatedActivityType.SHARE_ACCEPTED))
        assertTrue(types.contains(FederatedActivityType.SHARE_DECLINED))
        assertTrue(types.contains(FederatedActivityType.FILE_ACCESSED))
        assertTrue(types.contains(FederatedActivityType.FILE_MODIFIED))
        assertTrue(types.contains(FederatedActivityType.COMMENT_ADDED))
        assertTrue(types.contains(FederatedActivityType.INSTANCE_ONLINE))
        assertTrue(types.contains(FederatedActivityType.INSTANCE_OFFLINE))
    }

    @Test
    fun `FederationRequest should have correct structure`() {
        val request = FederationRequest(
            sourceInstance = "source.example.com",
            sourceName = "Source Storage",
            sourceVersion = "2.0.0",
            publicKey = "public-key-abc",
            capabilities = listOf(FederationCapability.SEND_SHARES),
            message = "Please accept federation",
        )

        assertEquals("source.example.com", request.sourceInstance)
        assertEquals("Source Storage", request.sourceName)
        assertEquals("2.0.0", request.sourceVersion)
        assertEquals("Please accept federation", request.message)
    }

    @Test
    fun `FederationResponse should indicate acceptance`() {
        val response = FederationResponse(
            accepted = true,
            instanceId = "instance-123",
            publicKey = "remote-public-key",
            capabilities = listOf(FederationCapability.RECEIVE_SHARES),
            message = "Welcome!",
        )

        assertTrue(response.accepted)
        assertEquals("instance-123", response.instanceId)
        assertEquals("Welcome!", response.message)
    }

    @Test
    fun `SignedFederationMessage should have signature`() {
        val now = Clock.System.now()
        val message = SignedFederationMessage(
            payload = """{"type":"ping"}""",
            signature = "signature-abc123",
            timestamp = now,
            nonce = "unique-nonce-123",
            senderDomain = "source.example.com",
        )

        assertEquals("""{"type":"ping"}""", message.payload)
        assertEquals("signature-abc123", message.signature)
        assertEquals("source.example.com", message.senderDomain)
    }
}
