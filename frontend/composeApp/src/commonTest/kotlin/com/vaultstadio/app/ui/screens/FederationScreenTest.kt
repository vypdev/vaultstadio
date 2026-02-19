/**
 * VaultStadio Federation Screen Tests
 */

package com.vaultstadio.app.ui.screens

import com.vaultstadio.app.domain.federation.model.FederatedIdentity
import com.vaultstadio.app.domain.federation.model.FederatedInstance
import com.vaultstadio.app.domain.federation.model.FederatedShare
import com.vaultstadio.app.domain.federation.model.FederatedShareStatus
import com.vaultstadio.app.domain.federation.model.FederationCapability
import com.vaultstadio.app.domain.federation.model.InstanceStatus
import com.vaultstadio.app.domain.federation.model.SharePermission
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FederationScreenTest {

    @Test
    fun testFederatedInstanceModel() {
        val now = Clock.System.now()
        val instance = FederatedInstance(
            id = "i1",
            domain = "vault.example.com",
            name = "Example Vault",
            description = "A test instance",
            version = "1.0.0",
            capabilities = listOf(FederationCapability.RECEIVE_SHARES, FederationCapability.SEND_SHARES),
            status = InstanceStatus.ONLINE,
            lastSeenAt = now,
            registeredAt = now,
        )

        assertEquals("vault.example.com", instance.domain)
        assertEquals(InstanceStatus.ONLINE, instance.status)
        assertTrue(instance.isOnline)
        assertEquals(2, instance.capabilities.size)
    }

    @Test
    fun testFederatedInstanceOffline() {
        val now = Clock.System.now()
        val instance = FederatedInstance(
            id = "i2",
            domain = "offline.example.com",
            name = "Offline Vault",
            description = null,
            version = "0.9.0",
            capabilities = emptyList(),
            status = InstanceStatus.OFFLINE,
            lastSeenAt = now,
            registeredAt = now,
        )

        assertEquals(InstanceStatus.OFFLINE, instance.status)
        assertFalse(instance.isOnline)
    }

    @Test
    fun testInstanceStatusEnumValues() {
        val statuses = InstanceStatus.entries
        assertEquals(5, statuses.size)
        assertTrue(statuses.contains(InstanceStatus.PENDING))
        assertTrue(statuses.contains(InstanceStatus.ONLINE))
        assertTrue(statuses.contains(InstanceStatus.OFFLINE))
        assertTrue(statuses.contains(InstanceStatus.BLOCKED))
        assertTrue(statuses.contains(InstanceStatus.REMOVED))
    }

    @Test
    fun testFederationCapabilityEnumValues() {
        val capabilities = FederationCapability.entries
        assertTrue(capabilities.contains(FederationCapability.RECEIVE_SHARES))
        assertTrue(capabilities.contains(FederationCapability.SEND_SHARES))
        assertTrue(capabilities.contains(FederationCapability.FEDERATED_IDENTITY))
    }

    @Test
    fun testFederatedShareModel() {
        val now = Clock.System.now()
        val share = FederatedShare(
            id = "fs1",
            itemId = "item1",
            sourceInstance = "vault1.example.com",
            targetInstance = "vault2.example.com",
            targetUserId = "user@vault2",
            permissions = listOf(SharePermission.READ, SharePermission.WRITE),
            status = FederatedShareStatus.PENDING,
            createdBy = "user1",
            createdAt = now,
        )

        assertEquals(FederatedShareStatus.PENDING, share.status)
        assertEquals(2, share.permissions.size)
    }

    @Test
    fun testFederatedShareStatusEnumValues() {
        val statuses = FederatedShareStatus.entries
        assertEquals(5, statuses.size)
        assertTrue(statuses.contains(FederatedShareStatus.PENDING))
        assertTrue(statuses.contains(FederatedShareStatus.ACCEPTED))
        assertTrue(statuses.contains(FederatedShareStatus.DECLINED))
        assertTrue(statuses.contains(FederatedShareStatus.REVOKED))
        assertTrue(statuses.contains(FederatedShareStatus.EXPIRED))
    }

    @Test
    fun testFederatedIdentityModel() {
        val now = Clock.System.now()
        val identity = FederatedIdentity(
            id = "fi1",
            localUserId = "local-user",
            remoteUserId = "remote-user",
            remoteInstance = "vault.example.com",
            displayName = "Remote User",
            email = "user@example.com",
            verified = true,
            linkedAt = now,
        )

        assertEquals("remote-user@vault.example.com", identity.federatedId)
        assertTrue(identity.verified)
    }

    @Test
    fun testFederatedIdentityUnverified() {
        val now = Clock.System.now()
        val identity = FederatedIdentity(
            id = "fi2",
            localUserId = "local-user",
            remoteUserId = "remote-user",
            remoteInstance = "other.example.com",
            displayName = "Another User",
            email = null,
            verified = false,
            linkedAt = now,
        )

        assertFalse(identity.verified)
        assertEquals(null, identity.email)
    }

    @Test
    fun testSharePermissionEnumValues() {
        val permissions = SharePermission.entries
        assertEquals(5, permissions.size)
        assertTrue(permissions.contains(SharePermission.READ))
        assertTrue(permissions.contains(SharePermission.WRITE))
        assertTrue(permissions.contains(SharePermission.DELETE))
        assertTrue(permissions.contains(SharePermission.SHARE))
        assertTrue(permissions.contains(SharePermission.ADMIN))
    }
}
