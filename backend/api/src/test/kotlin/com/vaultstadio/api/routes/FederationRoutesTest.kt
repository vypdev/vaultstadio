/**
 * VaultStadio Federation Routes Tests
 *
 * Unit tests for federation API data transfer objects.
 */

package com.vaultstadio.api.routes

import com.vaultstadio.api.routes.federation.CreateFederatedShareRequest
import com.vaultstadio.api.routes.federation.FederatedActivityResponse
import com.vaultstadio.api.routes.federation.FederatedIdentityResponse
import com.vaultstadio.api.routes.federation.FederatedInstanceResponse
import com.vaultstadio.api.routes.federation.FederatedShareResponse
import com.vaultstadio.api.routes.federation.FederationRequestBody
import com.vaultstadio.api.routes.federation.FederationResponseBody
import com.vaultstadio.api.routes.federation.IncomingFederationRequest
import com.vaultstadio.api.routes.federation.LinkIdentityRequest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FederationRoutesTest {

    // ========================================================================
    // FederationRequestBody Tests
    // ========================================================================

    @Test
    fun `FederationRequestBody should store target domain`() {
        val request = FederationRequestBody(
            targetDomain = "other-instance.example.com",
            message = null,
        )

        assertEquals("other-instance.example.com", request.targetDomain)
        assertNull(request.message)
    }

    @Test
    fun `FederationRequestBody should store optional message`() {
        val request = FederationRequestBody(
            targetDomain = "partner.example.com",
            message = "Hello, let's federate!",
        )

        assertEquals("partner.example.com", request.targetDomain)
        assertEquals("Hello, let's federate!", request.message)
    }

    // ========================================================================
    // FederatedInstanceResponse Tests
    // ========================================================================

    @Test
    fun `FederatedInstanceResponse should store all properties`() {
        val response = FederatedInstanceResponse(
            id = "instance-123",
            domain = "storage.example.com",
            name = "Example Storage",
            description = "A trusted storage instance",
            version = "2.0.0",
            capabilities = listOf("RECEIVE_SHARES", "SEND_SHARES"),
            status = "ACTIVE",
            lastSeenAt = "2024-01-15T10:30:00Z",
            registeredAt = "2024-01-01T00:00:00Z",
        )

        assertEquals("instance-123", response.id)
        assertEquals("storage.example.com", response.domain)
        assertEquals("Example Storage", response.name)
        assertEquals("A trusted storage instance", response.description)
        assertEquals("2.0.0", response.version)
        assertEquals(2, response.capabilities.size)
        assertTrue(response.capabilities.contains("RECEIVE_SHARES"))
        assertEquals("ACTIVE", response.status)
        assertEquals("2024-01-15T10:30:00Z", response.lastSeenAt)
        assertEquals("2024-01-01T00:00:00Z", response.registeredAt)
    }

    @Test
    fun `FederatedInstanceResponse should allow null description and lastSeenAt`() {
        val response = FederatedInstanceResponse(
            id = "instance-123",
            domain = "new-instance.com",
            name = "New Instance",
            description = null,
            version = "1.0.0",
            capabilities = emptyList(),
            status = "PENDING",
            lastSeenAt = null,
            registeredAt = "2024-01-15T10:00:00Z",
        )

        assertNull(response.description)
        assertNull(response.lastSeenAt)
        assertEquals("PENDING", response.status)
    }

    @Test
    fun `FederatedInstanceResponse should support various statuses`() {
        val active = FederatedInstanceResponse(
            id = "1", domain = "a.com", name = "A", description = null,
            version = "1.0", capabilities = emptyList(), status = "ACTIVE",
            lastSeenAt = null, registeredAt = "2024-01-01T00:00:00Z",
        )
        val pending = active.copy(id = "2", status = "PENDING")
        val blocked = active.copy(id = "3", status = "BLOCKED")
        val offline = active.copy(id = "4", status = "OFFLINE")

        assertEquals("ACTIVE", active.status)
        assertEquals("PENDING", pending.status)
        assertEquals("BLOCKED", blocked.status)
        assertEquals("OFFLINE", offline.status)
    }

    // ========================================================================
    // CreateFederatedShareRequest Tests
    // ========================================================================

    @Test
    fun `CreateFederatedShareRequest should store all properties`() {
        val request = CreateFederatedShareRequest(
            itemId = "item-123",
            targetInstance = "partner.example.com",
            targetUserId = "user@partner.example.com",
            permissions = listOf("READ", "WRITE"),
            expiresInDays = 30,
        )

        assertEquals("item-123", request.itemId)
        assertEquals("partner.example.com", request.targetInstance)
        assertEquals("user@partner.example.com", request.targetUserId)
        assertEquals(2, request.permissions.size)
        assertTrue(request.permissions.contains("READ"))
        assertTrue(request.permissions.contains("WRITE"))
        assertEquals(30, request.expiresInDays)
    }

    @Test
    fun `CreateFederatedShareRequest should have default values`() {
        val request = CreateFederatedShareRequest(
            itemId = "item-123",
            targetInstance = "other.example.com",
        )

        assertNull(request.targetUserId)
        assertEquals(listOf("READ"), request.permissions)
        assertNull(request.expiresInDays)
    }

    @Test
    fun `CreateFederatedShareRequest should support all permission types`() {
        val request = CreateFederatedShareRequest(
            itemId = "item-123",
            targetInstance = "partner.com",
            permissions = listOf("READ", "WRITE", "DELETE", "SHARE"),
        )

        assertEquals(4, request.permissions.size)
    }

    // ========================================================================
    // FederatedShareResponse Tests
    // ========================================================================

    @Test
    fun `FederatedShareResponse should store all properties`() {
        val response = FederatedShareResponse(
            id = "share-123",
            itemId = "item-456",
            sourceInstance = "source.example.com",
            targetInstance = "target.example.com",
            targetUserId = "user@target.com",
            permissions = listOf("READ"),
            status = "PENDING",
            expiresAt = "2024-02-15T00:00:00Z",
            createdBy = "user-789",
            createdAt = "2024-01-15T10:30:00Z",
            acceptedAt = null,
        )

        assertEquals("share-123", response.id)
        assertEquals("item-456", response.itemId)
        assertEquals("source.example.com", response.sourceInstance)
        assertEquals("target.example.com", response.targetInstance)
        assertEquals("user@target.com", response.targetUserId)
        assertEquals(listOf("READ"), response.permissions)
        assertEquals("PENDING", response.status)
        assertEquals("2024-02-15T00:00:00Z", response.expiresAt)
        assertEquals("user-789", response.createdBy)
        assertNull(response.acceptedAt)
    }

    @Test
    fun `FederatedShareResponse should support ACCEPTED status with acceptedAt`() {
        val response = FederatedShareResponse(
            id = "share-123",
            itemId = "item-456",
            sourceInstance = "source.com",
            targetInstance = "target.com",
            targetUserId = null,
            permissions = listOf("READ", "WRITE"),
            status = "ACCEPTED",
            expiresAt = null,
            createdBy = "user-123",
            createdAt = "2024-01-15T10:00:00Z",
            acceptedAt = "2024-01-15T11:00:00Z",
        )

        assertEquals("ACCEPTED", response.status)
        assertEquals("2024-01-15T11:00:00Z", response.acceptedAt)
        assertNull(response.expiresAt)
        assertNull(response.targetUserId)
    }

    @Test
    fun `FederatedShareResponse should support various statuses`() {
        val base = FederatedShareResponse(
            id = "1", itemId = "i", sourceInstance = "s", targetInstance = "t",
            targetUserId = null, permissions = listOf("READ"), status = "PENDING",
            expiresAt = null, createdBy = "u", createdAt = "2024-01-01T00:00:00Z",
            acceptedAt = null,
        )

        val pending = base.copy(status = "PENDING")
        val accepted = base.copy(status = "ACCEPTED")
        val declined = base.copy(status = "DECLINED")
        val revoked = base.copy(status = "REVOKED")
        val expired = base.copy(status = "EXPIRED")

        assertEquals("PENDING", pending.status)
        assertEquals("ACCEPTED", accepted.status)
        assertEquals("DECLINED", declined.status)
        assertEquals("REVOKED", revoked.status)
        assertEquals("EXPIRED", expired.status)
    }

    // ========================================================================
    // FederatedIdentityResponse Tests
    // ========================================================================

    @Test
    fun `FederatedIdentityResponse should store all properties`() {
        val response = FederatedIdentityResponse(
            id = "identity-123",
            localUserId = "local-user-456",
            remoteUserId = "remote-user@other.com",
            remoteInstance = "other.example.com",
            displayName = "John Doe",
            email = "john@other.com",
            avatarUrl = "https://other.com/avatar/john.jpg",
            verified = true,
            linkedAt = "2024-01-15T10:30:00Z",
        )

        assertEquals("identity-123", response.id)
        assertEquals("local-user-456", response.localUserId)
        assertEquals("remote-user@other.com", response.remoteUserId)
        assertEquals("other.example.com", response.remoteInstance)
        assertEquals("John Doe", response.displayName)
        assertEquals("john@other.com", response.email)
        assertEquals("https://other.com/avatar/john.jpg", response.avatarUrl)
        assertTrue(response.verified)
        assertEquals("2024-01-15T10:30:00Z", response.linkedAt)
    }

    @Test
    fun `FederatedIdentityResponse should allow null optional fields`() {
        val response = FederatedIdentityResponse(
            id = "identity-123",
            localUserId = null,
            remoteUserId = "remote@other.com",
            remoteInstance = "other.com",
            displayName = "Remote User",
            email = null,
            avatarUrl = null,
            verified = false,
            linkedAt = "2024-01-15T10:30:00Z",
        )

        assertNull(response.localUserId)
        assertNull(response.email)
        assertNull(response.avatarUrl)
        assertFalse(response.verified)
    }

    // ========================================================================
    // LinkIdentityRequest Tests
    // ========================================================================

    @Test
    fun `LinkIdentityRequest should store all properties`() {
        val request = LinkIdentityRequest(
            remoteUserId = "user@remote.example.com",
            remoteInstance = "remote.example.com",
            displayName = "Remote User",
        )

        assertEquals("user@remote.example.com", request.remoteUserId)
        assertEquals("remote.example.com", request.remoteInstance)
        assertEquals("Remote User", request.displayName)
    }

    // ========================================================================
    // FederatedActivityResponse Tests
    // ========================================================================

    @Test
    fun `FederatedActivityResponse should store all properties`() {
        val response = FederatedActivityResponse(
            id = "activity-123",
            instanceDomain = "partner.example.com",
            activityType = "SHARE_CREATED",
            actorId = "user-456",
            objectId = "share-789",
            objectType = "FederatedShare",
            summary = "User shared a file",
            timestamp = "2024-01-15T10:30:00Z",
        )

        assertEquals("activity-123", response.id)
        assertEquals("partner.example.com", response.instanceDomain)
        assertEquals("SHARE_CREATED", response.activityType)
        assertEquals("user-456", response.actorId)
        assertEquals("share-789", response.objectId)
        assertEquals("FederatedShare", response.objectType)
        assertEquals("User shared a file", response.summary)
        assertEquals("2024-01-15T10:30:00Z", response.timestamp)
    }

    @Test
    fun `FederatedActivityResponse should support various activity types`() {
        val base = FederatedActivityResponse(
            id = "1",
            instanceDomain = "d",
            activityType = "SHARE_CREATED",
            actorId = "a",
            objectId = "o",
            objectType = "t",
            summary = "s",
            timestamp = "2024-01-01T00:00:00Z",
        )

        val created = base.copy(activityType = "SHARE_CREATED")
        val accepted = base.copy(activityType = "SHARE_ACCEPTED")
        val declined = base.copy(activityType = "SHARE_DECLINED")
        val revoked = base.copy(activityType = "SHARE_REVOKED")

        assertEquals("SHARE_CREATED", created.activityType)
        assertEquals("SHARE_ACCEPTED", accepted.activityType)
        assertEquals("SHARE_DECLINED", declined.activityType)
        assertEquals("SHARE_REVOKED", revoked.activityType)
    }

    // ========================================================================
    // IncomingFederationRequest Tests
    // ========================================================================

    @Test
    fun `IncomingFederationRequest should store all properties`() {
        val request = IncomingFederationRequest(
            sourceInstance = "requester.example.com",
            sourceName = "Requester Instance",
            sourceVersion = "2.0.0",
            publicKey = "-----BEGIN PUBLIC KEY-----\nMIIBIjANBg...\n-----END PUBLIC KEY-----",
            capabilities = listOf("RECEIVE_SHARES", "SEND_SHARES", "FEDERATED_IDENTITY"),
            message = "Requesting federation for file sharing",
        )

        assertEquals("requester.example.com", request.sourceInstance)
        assertEquals("Requester Instance", request.sourceName)
        assertEquals("2.0.0", request.sourceVersion)
        assertTrue(request.publicKey.contains("PUBLIC KEY"))
        assertEquals(3, request.capabilities.size)
        assertEquals("Requesting federation for file sharing", request.message)
    }

    @Test
    fun `IncomingFederationRequest should allow null message`() {
        val request = IncomingFederationRequest(
            sourceInstance = "other.com",
            sourceName = "Other",
            sourceVersion = "1.0.0",
            publicKey = "key",
            capabilities = emptyList(),
            message = null,
        )

        assertNull(request.message)
    }

    // ========================================================================
    // FederationResponseBody Tests
    // ========================================================================

    @Test
    fun `FederationResponseBody should store accepted response`() {
        val response = FederationResponseBody(
            accepted = true,
            instanceId = "instance-123",
            publicKey = "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----",
            capabilities = listOf("RECEIVE_SHARES", "SEND_SHARES"),
            message = "Federation accepted",
        )

        assertTrue(response.accepted)
        assertEquals("instance-123", response.instanceId)
        assertNotNull(response.publicKey)
        assertEquals(2, response.capabilities.size)
        assertEquals("Federation accepted", response.message)
    }

    @Test
    fun `FederationResponseBody should store rejected response`() {
        val response = FederationResponseBody(
            accepted = false,
            instanceId = null,
            publicKey = null,
            capabilities = emptyList(),
            message = "Federation request rejected: policy violation",
        )

        assertFalse(response.accepted)
        assertNull(response.instanceId)
        assertNull(response.publicKey)
        assertTrue(response.capabilities.isEmpty())
        assertEquals("Federation request rejected: policy violation", response.message)
    }

    // ========================================================================
    // Edge Cases and Validation
    // ========================================================================

    @Test
    fun `FederatedInstanceResponse should handle empty capabilities`() {
        val response = FederatedInstanceResponse(
            id = "1",
            domain = "minimal.com",
            name = "Minimal",
            description = null,
            version = "1.0.0",
            capabilities = emptyList(),
            status = "ACTIVE",
            lastSeenAt = null,
            registeredAt = "2024-01-01T00:00:00Z",
        )

        assertTrue(response.capabilities.isEmpty())
    }

    @Test
    fun `CreateFederatedShareRequest should handle single permission`() {
        val request = CreateFederatedShareRequest(
            itemId = "item-1",
            targetInstance = "target.com",
            permissions = listOf("READ"),
        )

        assertEquals(1, request.permissions.size)
        assertEquals("READ", request.permissions[0])
    }

    @Test
    fun `FederatedShareResponse should handle empty permissions list`() {
        val response = FederatedShareResponse(
            id = "share-1",
            itemId = "item-1",
            sourceInstance = "s.com",
            targetInstance = "t.com",
            targetUserId = null,
            permissions = emptyList(),
            status = "PENDING",
            expiresAt = null,
            createdBy = "user-1",
            createdAt = "2024-01-01T00:00:00Z",
            acceptedAt = null,
        )

        assertTrue(response.permissions.isEmpty())
    }

    @Test
    fun `FederatedIdentityResponse verified flag should be settable`() {
        val verified = FederatedIdentityResponse(
            id = "1", localUserId = null, remoteUserId = "r", remoteInstance = "i",
            displayName = "D", email = null, avatarUrl = null, verified = true,
            linkedAt = "2024-01-01T00:00:00Z",
        )
        val unverified = verified.copy(verified = false)

        assertTrue(verified.verified)
        assertFalse(unverified.verified)
    }

    @Test
    fun `IncomingFederationRequest capabilities should support all types`() {
        val request = IncomingFederationRequest(
            sourceInstance = "full.com",
            sourceName = "Full Instance",
            sourceVersion = "2.0.0",
            publicKey = "key",
            capabilities = listOf(
                "RECEIVE_SHARES",
                "SEND_SHARES",
                "FEDERATED_IDENTITY",
                "ACTIVITY_STREAM",
            ),
            message = null,
        )

        assertEquals(4, request.capabilities.size)
        assertTrue(request.capabilities.contains("RECEIVE_SHARES"))
        assertTrue(request.capabilities.contains("SEND_SHARES"))
        assertTrue(request.capabilities.contains("FEDERATED_IDENTITY"))
        assertTrue(request.capabilities.contains("ACTIVITY_STREAM"))
    }
}
