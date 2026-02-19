/**
 * VaultStadio Federation Models
 *
 * Models for federating multiple VaultStadio instances.
 * Enables cross-instance sharing, discovery, and identity.
 */

package com.vaultstadio.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a federated VaultStadio instance.
 *
 * @property id Unique identifier for this instance
 * @property domain Domain name of the instance (e.g., "storage.example.com")
 * @property name Human-readable name
 * @property description Instance description
 * @property version VaultStadio version running on this instance
 * @property publicKey Public key for signature verification
 * @property capabilities List of supported features
 * @property status Current status
 * @property lastSeenAt Last successful communication
 * @property registeredAt When this instance was registered
 * @property metadata Additional instance metadata
 */
@Serializable
data class FederatedInstance(
    val id: String = UUID.randomUUID().toString(),
    val domain: String,
    val name: String,
    val description: String? = null,
    val version: String,
    val publicKey: String,
    val capabilities: List<FederationCapability> = emptyList(),
    val status: InstanceStatus = InstanceStatus.PENDING,
    val lastSeenAt: Instant? = null,
    val registeredAt: Instant,
    val metadata: Map<String, String> = emptyMap(),
) {
    /**
     * Returns the full instance URI.
     */
    val uri: String get() = "vaultstadio://$domain"

    /**
     * Checks if the instance is reachable.
     */
    val isOnline: Boolean get() = status == InstanceStatus.ONLINE
}

/**
 * Status of a federated instance.
 */
@Serializable
enum class InstanceStatus {
    /** Pending approval */
    PENDING,

    /** Online and reachable */
    ONLINE,

    /** Temporarily unreachable */
    OFFLINE,

    /** Permanently blocked */
    BLOCKED,

    /** Instance has been removed */
    REMOVED,
}

/**
 * Capabilities that can be supported by a federated instance.
 */
@Serializable
enum class FederationCapability {
    /** Can receive shared files */
    RECEIVE_SHARES,

    /** Can send shared files */
    SEND_SHARES,

    /** Supports federated identity */
    FEDERATED_IDENTITY,

    /** Supports cross-instance search */
    FEDERATED_SEARCH,

    /** Supports activity streams */
    ACTIVITY_STREAM,

    /** Supports real-time events */
    REAL_TIME_EVENTS,
}

/**
 * Represents a federated share (share across instances).
 *
 * @property id Unique identifier
 * @property itemId Local storage item ID
 * @property sourceInstance Origin instance domain
 * @property targetInstance Destination instance domain
 * @property targetUserId User ID on target instance (optional)
 * @property permissions Granted permissions
 * @property expiresAt When the share expires
 * @property createdBy User who created the share
 * @property createdAt When the share was created
 * @property acceptedAt When the share was accepted
 * @property status Current share status
 */
@Serializable
data class FederatedShare(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val sourceInstance: String,
    val targetInstance: String,
    val targetUserId: String? = null,
    val permissions: List<SharePermission> = listOf(SharePermission.READ),
    val expiresAt: Instant? = null,
    val createdBy: String,
    val createdAt: Instant,
    val acceptedAt: Instant? = null,
    val status: FederatedShareStatus = FederatedShareStatus.PENDING,
)

/**
 * Permissions for a federated share.
 */
@Serializable
enum class SharePermission {
    /** Can view/download the file */
    READ,

    /** Can modify the file */
    WRITE,

    /** Can delete the file */
    DELETE,

    /** Can re-share the file */
    SHARE,

    /** Full control */
    ADMIN,
}

/**
 * Status of a federated share.
 */
@Serializable
enum class FederatedShareStatus {
    /** Waiting for acceptance */
    PENDING,

    /** Share accepted and active */
    ACCEPTED,

    /** Share declined */
    DECLINED,

    /** Share revoked by owner */
    REVOKED,

    /** Share expired */
    EXPIRED,
}

/**
 * Represents a federated user identity.
 *
 * @property id Unique identifier
 * @property localUserId Local user ID (if linked)
 * @property remoteUserId User ID on remote instance
 * @property remoteInstance Remote instance domain
 * @property displayName Display name from remote
 * @property email Email (if shared)
 * @property avatarUrl Avatar URL
 * @property verified Whether identity is verified
 * @property linkedAt When the identity was linked
 */
@Serializable
data class FederatedIdentity(
    val id: String = UUID.randomUUID().toString(),
    val localUserId: String? = null,
    val remoteUserId: String,
    val remoteInstance: String,
    val displayName: String,
    val email: String? = null,
    val avatarUrl: String? = null,
    val verified: Boolean = false,
    val linkedAt: Instant,
) {
    /**
     * Full federated ID (user@instance).
     */
    val federatedId: String get() = "$remoteUserId@$remoteInstance"
}

/**
 * Activity from a federated instance.
 *
 * @property id Unique identifier
 * @property instanceDomain Source instance
 * @property activityType Type of activity
 * @property actorId Actor federated ID
 * @property objectId Object being acted upon
 * @property objectType Type of object
 * @property summary Human-readable summary
 * @property timestamp When the activity occurred
 * @property metadata Additional activity data
 */
@Serializable
data class FederatedActivity(
    val id: String = UUID.randomUUID().toString(),
    val instanceDomain: String,
    val activityType: FederatedActivityType,
    val actorId: String,
    val objectId: String,
    val objectType: String,
    val summary: String,
    val timestamp: Instant,
    val metadata: Map<String, String> = emptyMap(),
)

/**
 * Type of federated activity.
 */
@Serializable
enum class FederatedActivityType {
    /** Share was created */
    SHARE_CREATED,

    /** Share was accepted */
    SHARE_ACCEPTED,

    /** Share was declined */
    SHARE_DECLINED,

    /** File was accessed */
    FILE_ACCESSED,

    /** File was modified */
    FILE_MODIFIED,

    /** Comment was added */
    COMMENT_ADDED,

    /** Instance came online */
    INSTANCE_ONLINE,

    /** Instance went offline */
    INSTANCE_OFFLINE,
}

/**
 * Request to establish federation with another instance.
 *
 * @property sourceInstance Requesting instance domain
 * @property sourceName Requesting instance name
 * @property sourceVersion VaultStadio version
 * @property publicKey Public key for verification
 * @property capabilities Requested capabilities
 * @property message Optional message to administrator
 */
@Serializable
data class FederationRequest(
    val sourceInstance: String,
    val sourceName: String,
    val sourceVersion: String,
    val publicKey: String,
    val capabilities: List<FederationCapability>,
    val message: String? = null,
)

/**
 * Response to a federation request.
 *
 * @property accepted Whether the request was accepted
 * @property instanceId Assigned instance ID (if accepted)
 * @property publicKey This instance's public key
 * @property capabilities Agreed capabilities
 * @property message Response message
 */
@Serializable
data class FederationResponse(
    val accepted: Boolean,
    val instanceId: String? = null,
    val publicKey: String? = null,
    val capabilities: List<FederationCapability> = emptyList(),
    val message: String? = null,
)

/**
 * Signed message for federation communication.
 *
 * @property payload The message payload (JSON)
 * @property signature Digital signature (Base64 encoded)
 * @property timestamp Message timestamp
 * @property nonce Unique nonce to prevent replay
 * @property senderDomain Sender instance domain
 * @property algorithm Signing algorithm (Ed25519 or SHA256withRSA)
 */
@Serializable
data class SignedFederationMessage(
    val payload: String,
    val signature: String,
    val timestamp: Instant,
    val nonce: String,
    val senderDomain: String,
    val algorithm: String? = "Ed25519",
)
