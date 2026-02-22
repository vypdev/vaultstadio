/**
 * Unit tests for federation domain models: FederatedInstance, InstanceStatus,
 * FederationCapability, FederatedIdentity, FederatedActivity, FederatedActivityType.
 */

package com.vaultstadio.app.domain.federation

import com.vaultstadio.app.domain.federation.model.FederatedActivity
import com.vaultstadio.app.domain.federation.model.FederatedActivityType
import com.vaultstadio.app.domain.federation.model.FederatedIdentity
import com.vaultstadio.app.domain.federation.model.FederatedInstance
import com.vaultstadio.app.domain.federation.model.FederationCapability
import com.vaultstadio.app.domain.federation.model.InstanceStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.Instant

class FederatedInstanceTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun federatedInstance_construction() {
        val instance = FederatedInstance(
            id = "inst-1",
            domain = "node.example.com",
            name = "Example Node",
            description = "A test instance",
            version = "1.0.0",
            capabilities = listOf(FederationCapability.RECEIVE_SHARES, FederationCapability.SEND_SHARES),
            status = InstanceStatus.ONLINE,
            lastSeenAt = testInstant,
            registeredAt = testInstant,
        )
        assertEquals("inst-1", instance.id)
        assertEquals("node.example.com", instance.domain)
        assertEquals("Example Node", instance.name)
        assertEquals("1.0.0", instance.version)
        assertEquals(2, instance.capabilities.size)
        assertEquals(InstanceStatus.ONLINE, instance.status)
        assertTrue(instance.isOnline)
    }

    @Test
    fun federatedInstance_isOnlineFalseWhenOffline() {
        val instance = FederatedInstance(
            id = "i2",
            domain = "offline.example.com",
            name = "Offline",
            version = "1.0",
            capabilities = emptyList(),
            status = InstanceStatus.OFFLINE,
            registeredAt = testInstant,
        )
        assertFalse(instance.isOnline)
        assertNull(instance.description)
        assertNull(instance.lastSeenAt)
    }
}

class InstanceStatusTest {

    @Test
    fun instanceStatus_hasExpectedValues() {
        val values = InstanceStatus.entries
        assertTrue(InstanceStatus.PENDING in values)
        assertTrue(InstanceStatus.ONLINE in values)
        assertTrue(InstanceStatus.OFFLINE in values)
        assertTrue(InstanceStatus.BLOCKED in values)
        assertTrue(InstanceStatus.REMOVED in values)
        assertEquals(5, values.size)
    }

    @Test
    fun instanceStatus_names() {
        assertEquals("ONLINE", InstanceStatus.ONLINE.name)
        assertEquals("OFFLINE", InstanceStatus.OFFLINE.name)
    }

    @Test
    fun instanceStatus_allNames() {
        assertEquals("PENDING", InstanceStatus.PENDING.name)
        assertEquals("BLOCKED", InstanceStatus.BLOCKED.name)
        assertEquals("REMOVED", InstanceStatus.REMOVED.name)
    }
}

class FederationCapabilityTest {

    @Test
    fun federationCapability_hasExpectedValues() {
        val values = FederationCapability.entries
        assertTrue(FederationCapability.RECEIVE_SHARES in values)
        assertTrue(FederationCapability.SEND_SHARES in values)
        assertTrue(FederationCapability.FEDERATED_IDENTITY in values)
        assertTrue(FederationCapability.FEDERATED_SEARCH in values)
        assertTrue(FederationCapability.ACTIVITY_STREAM in values)
        assertTrue(FederationCapability.REAL_TIME_EVENTS in values)
        assertEquals(6, values.size)
    }

    @Test
    fun federationCapability_names() {
        assertEquals("RECEIVE_SHARES", FederationCapability.RECEIVE_SHARES.name)
        assertEquals("SEND_SHARES", FederationCapability.SEND_SHARES.name)
    }
}

class FederatedIdentityTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun federatedIdentity_construction() {
        val identity = FederatedIdentity(
            id = "id-1",
            localUserId = "local-u1",
            remoteUserId = "remote-123",
            remoteInstance = "node.example.com",
            displayName = "Alice",
            email = "alice@example.com",
            avatarUrl = null,
            verified = true,
            linkedAt = testInstant,
        )
        assertEquals("id-1", identity.id)
        assertEquals("local-u1", identity.localUserId)
        assertEquals("remote-123", identity.remoteUserId)
        assertEquals("node.example.com", identity.remoteInstance)
        assertEquals("Alice", identity.displayName)
        assertTrue(identity.verified)
        assertEquals("remote-123@node.example.com", identity.federatedId)
    }

    @Test
    fun federatedIdentity_federatedIdFormat() {
        val identity = FederatedIdentity(
            id = "i2",
            remoteUserId = "u",
            remoteInstance = "host",
            displayName = "User",
            verified = false,
            linkedAt = testInstant,
        )
        assertEquals("u@host", identity.federatedId)
        assertNull(identity.localUserId)
        assertNull(identity.email)
    }
}

class FederatedActivityTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun federatedActivity_construction() {
        val activity = FederatedActivity(
            id = "act-1",
            instanceDomain = "node.example.com",
            activityType = FederatedActivityType.SHARE_CREATED,
            actorId = "actor-1",
            objectId = "obj-1",
            objectType = "Share",
            summary = "Share created",
            timestamp = testInstant,
        )
        assertEquals("act-1", activity.id)
        assertEquals("node.example.com", activity.instanceDomain)
        assertEquals(FederatedActivityType.SHARE_CREATED, activity.activityType)
        assertEquals("Share created", activity.summary)
    }
}

class FederatedActivityTypeTest {

    @Test
    fun federatedActivityType_hasExpectedValues() {
        val values = FederatedActivityType.entries
        assertTrue(FederatedActivityType.SHARE_CREATED in values)
        assertTrue(FederatedActivityType.FILE_ACCESSED in values)
        assertTrue(FederatedActivityType.INSTANCE_ONLINE in values)
        assertEquals(8, values.size)
    }

    @Test
    fun federatedActivityType_names() {
        assertEquals("SHARE_CREATED", FederatedActivityType.SHARE_CREATED.name)
        assertEquals("FILE_ACCESSED", FederatedActivityType.FILE_ACCESSED.name)
    }
}
