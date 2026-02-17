/**
 * VaultStadio Advanced Feature Events
 *
 * Events for versioning, sync, collaboration, and federation.
 */

package com.vaultstadio.core.domain.event

import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.ConflictResolution
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.core.domain.model.FederatedShare
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.model.SyncChange
import com.vaultstadio.core.domain.model.SyncConflict
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Base interface for all domain events.
 */
interface DomainEvent {
    val id: String
    val timestamp: Instant
    val userId: String
}

// ============================================================================
// File Version Events
// ============================================================================

/**
 * Events related to file versioning.
 */
@Serializable
sealed class VersionEvent : DomainEvent {

    /**
     * A new version was created.
     */
    @Serializable
    data class Created(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val version: FileVersion,
    ) : VersionEvent()

    /**
     * A version was restored.
     */
    @Serializable
    data class Restored(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val itemId: String,
        val fromVersion: Int,
        val toVersion: Int,
    ) : VersionEvent()

    /**
     * A version was deleted.
     */
    @Serializable
    data class Deleted(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val versionId: String,
        val itemId: String,
        val versionNumber: Int,
    ) : VersionEvent()

    /**
     * Retention policy was applied.
     */
    @Serializable
    data class RetentionApplied(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val itemId: String,
        val deletedVersions: List<String>,
    ) : VersionEvent()
}

// ============================================================================
// Sync Events
// ============================================================================

/**
 * Events related to file synchronization.
 */
@Serializable
sealed class SyncEvent : DomainEvent {

    /**
     * A device was registered for sync.
     */
    @Serializable
    data class DeviceRegistered(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val deviceId: String,
        val deviceName: String,
    ) : SyncEvent()

    /**
     * A device was deactivated.
     */
    @Serializable
    data class DeviceDeactivated(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val deviceId: String,
    ) : SyncEvent()

    /**
     * A sync was completed.
     */
    @Serializable
    data class SyncCompleted(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val deviceId: String,
        val changeCount: Int,
        val cursor: String,
    ) : SyncEvent()

    /**
     * A change was recorded.
     */
    @Serializable
    data class ChangeRecorded(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val change: SyncChange,
    ) : SyncEvent()

    /**
     * A conflict was detected.
     */
    @Serializable
    data class ConflictDetected(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val conflict: SyncConflict,
    ) : SyncEvent()

    /**
     * A conflict was resolved.
     */
    @Serializable
    data class ConflictResolved(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val conflictId: String,
        val resolution: ConflictResolution,
    ) : SyncEvent()
}

// ============================================================================
// Collaboration Events
// ============================================================================

/**
 * Events related to real-time collaboration.
 */
@Serializable
sealed class CollabEvent : DomainEvent {

    /**
     * A collaboration session was started.
     */
    @Serializable
    data class SessionStarted(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val sessionId: String,
        val itemId: String,
    ) : CollabEvent()

    /**
     * A collaboration session ended.
     */
    @Serializable
    data class SessionEnded(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val sessionId: String,
        val itemId: String,
    ) : CollabEvent()

    /**
     * A participant joined a session.
     */
    @Serializable
    data class ParticipantJoined(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val sessionId: String,
        val participant: CollaborationParticipant,
    ) : CollabEvent()

    /**
     * A participant left a session.
     */
    @Serializable
    data class ParticipantLeft(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val sessionId: String,
        val participantId: String,
    ) : CollabEvent()

    /**
     * An operation was applied.
     */
    @Serializable
    data class OperationApplied(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val sessionId: String,
        val itemId: String,
        val operation: CollaborationOperation,
        val newVersion: Long,
    ) : CollabEvent()

    /**
     * A document was saved.
     */
    @Serializable
    data class DocumentSaved(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val itemId: String,
        val version: Long,
    ) : CollabEvent()

    /**
     * A comment was added.
     */
    @Serializable
    data class CommentAdded(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val comment: DocumentComment,
    ) : CollabEvent()

    /**
     * A comment was resolved.
     */
    @Serializable
    data class CommentResolved(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val commentId: String,
        val itemId: String,
    ) : CollabEvent()
}

// ============================================================================
// Federation Events
// ============================================================================

/**
 * Events related to federation.
 */
@Serializable
sealed class FederationEvent : DomainEvent {

    /**
     * Federation was requested with another instance.
     */
    @Serializable
    data class FederationRequested(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val targetDomain: String,
    ) : FederationEvent()

    /**
     * Federation was accepted.
     */
    @Serializable
    data class FederationAccepted(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val instanceDomain: String,
        val instanceName: String,
    ) : FederationEvent()

    /**
     * An instance was blocked.
     */
    @Serializable
    data class InstanceBlocked(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val instanceDomain: String,
    ) : FederationEvent()

    /**
     * An instance went online.
     */
    @Serializable
    data class InstanceOnline(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val instanceDomain: String,
    ) : FederationEvent()

    /**
     * An instance went offline.
     */
    @Serializable
    data class InstanceOffline(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val instanceDomain: String,
    ) : FederationEvent()

    /**
     * A federated share was created.
     */
    @Serializable
    data class ShareCreated(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val share: FederatedShare,
    ) : FederationEvent()

    /**
     * A federated share was accepted.
     */
    @Serializable
    data class ShareAccepted(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val shareId: String,
    ) : FederationEvent()

    /**
     * A federated share was declined.
     */
    @Serializable
    data class ShareDeclined(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val shareId: String,
    ) : FederationEvent()

    /**
     * A federated share was revoked.
     */
    @Serializable
    data class ShareRevoked(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val shareId: String,
    ) : FederationEvent()

    /**
     * A federated identity was linked.
     */
    @Serializable
    data class IdentityLinked(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val remoteUserId: String,
        val remoteInstance: String,
    ) : FederationEvent()
}
