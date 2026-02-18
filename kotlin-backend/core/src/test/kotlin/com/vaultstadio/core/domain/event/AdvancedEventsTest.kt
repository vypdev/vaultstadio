/**
 * Tests for AdvancedEvents (VersionEvent, SyncEvent, CollabEvent, FederationEvent).
 * Verifies instantiation and property access to improve domain.event package coverage.
 */

package com.vaultstadio.core.domain.event

import com.vaultstadio.core.domain.model.ChangeType
import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.CommentAnchor
import com.vaultstadio.core.domain.model.ConflictResolution
import com.vaultstadio.core.domain.model.ConflictType
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.core.domain.model.FederatedShare
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.model.SharePermission
import com.vaultstadio.core.domain.model.SyncChange
import com.vaultstadio.core.domain.model.SyncConflict
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AdvancedEventsTest {

    private val now: Instant = Clock.System.now()

    // ---------- VersionEvent ----------

    @Test
    fun `VersionEvent Created`() {
        val version = FileVersion(
            itemId = "item-1",
            versionNumber = 1,
            size = 100,
            checksum = "abc",
            storageKey = "key",
            createdBy = "u1",
            createdAt = now,
        )
        val e = VersionEvent.Created(userId = "u1", version = version)
        assertEquals(version, e.version)
        assertEquals("u1", e.userId)
    }

    @Test
    fun `VersionEvent Restored`() {
        val e = VersionEvent.Restored(userId = "u1", itemId = "item-1", fromVersion = 1, toVersion = 2)
        assertEquals("item-1", e.itemId)
        assertEquals(1, e.fromVersion)
        assertEquals(2, e.toVersion)
    }

    @Test
    fun `VersionEvent Deleted`() {
        val e = VersionEvent.Deleted(
            userId = "u1",
            versionId = "v1",
            itemId = "item-1",
            versionNumber = 3,
        )
        assertEquals("v1", e.versionId)
        assertEquals(3, e.versionNumber)
    }

    @Test
    fun `VersionEvent RetentionApplied`() {
        val e = VersionEvent.RetentionApplied(
            userId = "u1",
            itemId = "item-1",
            deletedVersions = listOf("v1", "v2"),
        )
        assertEquals(listOf("v1", "v2"), e.deletedVersions)
    }

    // ---------- SyncEvent ----------

    @Test
    fun `SyncEvent DeviceRegistered`() {
        val e = SyncEvent.DeviceRegistered(userId = "u1", deviceId = "d1", deviceName = "Laptop")
        assertEquals("d1", e.deviceId)
        assertEquals("Laptop", e.deviceName)
    }

    @Test
    fun `SyncEvent DeviceDeactivated`() {
        val e = SyncEvent.DeviceDeactivated(userId = "u1", deviceId = "d1")
        assertEquals("d1", e.deviceId)
    }

    @Test
    fun `SyncEvent SyncCompleted`() {
        val e = SyncEvent.SyncCompleted(userId = "u1", deviceId = "d1", changeCount = 5, cursor = "c1")
        assertEquals(5, e.changeCount)
        assertEquals("c1", e.cursor)
    }

    @Test
    fun `SyncEvent ChangeRecorded`() {
        val change = SyncChange(
            itemId = "item-1",
            changeType = ChangeType.MODIFY,
            userId = "u1",
            timestamp = now,
            cursor = 1,
        )
        val e = SyncEvent.ChangeRecorded(userId = "u1", change = change)
        assertEquals(change, e.change)
    }

    @Test
    fun `SyncEvent ConflictDetected`() {
        val local = SyncChange(itemId = "i1", changeType = ChangeType.MODIFY, userId = "u1", timestamp = now, cursor = 1)
        val remote = SyncChange(itemId = "i1", changeType = ChangeType.MODIFY, userId = "u2", timestamp = now, cursor = 2)
        val conflict = SyncConflict(
            itemId = "i1",
            localChange = local,
            remoteChange = remote,
            conflictType = ConflictType.EDIT_CONFLICT,
            createdAt = now,
        )
        val e = SyncEvent.ConflictDetected(userId = "u1", conflict = conflict)
        assertEquals(conflict, e.conflict)
    }

    @Test
    fun `SyncEvent ConflictResolved`() {
        val e = SyncEvent.ConflictResolved(
            userId = "u1",
            conflictId = "c1",
            resolution = ConflictResolution.KEEP_LOCAL,
        )
        assertEquals("c1", e.conflictId)
        assertEquals(ConflictResolution.KEEP_LOCAL, e.resolution)
    }

    // ---------- CollabEvent ----------

    @Test
    fun `CollabEvent SessionStarted`() {
        val e = CollabEvent.SessionStarted(userId = "u1", sessionId = "s1", itemId = "item-1")
        assertEquals("s1", e.sessionId)
        assertEquals("item-1", e.itemId)
    }

    @Test
    fun `CollabEvent SessionEnded`() {
        val e = CollabEvent.SessionEnded(userId = "u1", sessionId = "s1", itemId = "item-1")
        assertEquals("s1", e.sessionId)
    }

    @Test
    fun `CollabEvent ParticipantJoined`() {
        val participant = CollaborationParticipant(
            userId = "u1",
            userName = "Alice",
            color = "#ff0000",
            joinedAt = now,
            lastActiveAt = now,
        )
        val e = CollabEvent.ParticipantJoined(userId = "u1", sessionId = "s1", participant = participant)
        assertEquals(participant, e.participant)
    }

    @Test
    fun `CollabEvent ParticipantLeft`() {
        val e = CollabEvent.ParticipantLeft(userId = "u1", sessionId = "s1", participantId = "p1")
        assertEquals("p1", e.participantId)
    }

    @Test
    fun `CollabEvent OperationApplied`() {
        val op = CollaborationOperation.Insert(userId = "u1", timestamp = now, baseVersion = 1, position = 0, text = "hi")
        val e = CollabEvent.OperationApplied(
            userId = "u1",
            sessionId = "s1",
            itemId = "item-1",
            operation = op,
            newVersion = 2,
        )
        assertEquals(op, e.operation)
        assertEquals(2, e.newVersion)
    }

    @Test
    fun `CollabEvent DocumentSaved`() {
        val e = CollabEvent.DocumentSaved(userId = "u1", itemId = "item-1", version = 5)
        assertEquals(5, e.version)
    }

    @Test
    fun `CollabEvent CommentAdded`() {
        val anchor = CommentAnchor(startLine = 0, startColumn = 0, endLine = 0, endColumn = 5)
        val comment = DocumentComment(
            itemId = "item-1",
            userId = "u1",
            content = "Hello",
            anchor = anchor,
            createdAt = now,
            updatedAt = now,
        )
        val e = CollabEvent.CommentAdded(userId = "u1", comment = comment)
        assertEquals(comment, e.comment)
    }

    @Test
    fun `CollabEvent CommentResolved`() {
        val e = CollabEvent.CommentResolved(userId = "u1", commentId = "c1", itemId = "item-1")
        assertEquals("c1", e.commentId)
        assertEquals("item-1", e.itemId)
    }

    // ---------- FederationEvent ----------

    @Test
    fun `FederationEvent FederationRequested`() {
        val e = FederationEvent.FederationRequested(userId = "u1", targetDomain = "other.example.com")
        assertEquals("other.example.com", e.targetDomain)
    }

    @Test
    fun `FederationEvent FederationAccepted`() {
        val e = FederationEvent.FederationAccepted(
            userId = "u1",
            instanceDomain = "other.com",
            instanceName = "Other Instance",
        )
        assertEquals("Other Instance", e.instanceName)
    }

    @Test
    fun `FederationEvent InstanceBlocked`() {
        val e = FederationEvent.InstanceBlocked(userId = "u1", instanceDomain = "bad.com")
        assertEquals("bad.com", e.instanceDomain)
    }

    @Test
    fun `FederationEvent InstanceOnline`() {
        val e = FederationEvent.InstanceOnline(userId = "u1", instanceDomain = "node.com")
        assertEquals("node.com", e.instanceDomain)
    }

    @Test
    fun `FederationEvent InstanceOffline`() {
        val e = FederationEvent.InstanceOffline(userId = "u1", instanceDomain = "node.com")
        assertNotNull(e.timestamp)
    }

    @Test
    fun `FederationEvent ShareCreated`() {
        val share = FederatedShare(
            itemId = "item-1",
            sourceInstance = "a.com",
            targetInstance = "b.com",
            createdBy = "u1",
            createdAt = now,
            permissions = listOf(SharePermission.READ),
        )
        val e = FederationEvent.ShareCreated(userId = "u1", share = share)
        assertEquals(share, e.share)
    }

    @Test
    fun `FederationEvent ShareAccepted`() {
        val e = FederationEvent.ShareAccepted(userId = "u1", shareId = "share-1")
        assertEquals("share-1", e.shareId)
    }

    @Test
    fun `FederationEvent ShareDeclined`() {
        val e = FederationEvent.ShareDeclined(userId = "u1", shareId = "share-1")
        assertEquals("share-1", e.shareId)
    }

    @Test
    fun `FederationEvent ShareRevoked`() {
        val e = FederationEvent.ShareRevoked(userId = "u1", shareId = "share-1")
        assertEquals("share-1", e.shareId)
    }

    @Test
    fun `FederationEvent IdentityLinked`() {
        val e = FederationEvent.IdentityLinked(
            userId = "u1",
            remoteUserId = "remote-u1",
            remoteInstance = "fed.com",
        )
        assertEquals("remote-u1", e.remoteUserId)
        assertEquals("fed.com", e.remoteInstance)
    }
}
