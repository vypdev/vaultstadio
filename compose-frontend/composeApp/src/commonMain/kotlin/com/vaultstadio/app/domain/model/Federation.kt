/**
 * Federation Domain Models
 */

package com.vaultstadio.app.domain.model

import kotlinx.datetime.Instant

enum class InstanceStatus { PENDING, ONLINE, OFFLINE, BLOCKED, REMOVED }
enum class FederationCapability {
    RECEIVE_SHARES,
    SEND_SHARES,
    FEDERATED_IDENTITY,
    FEDERATED_SEARCH,
    ACTIVITY_STREAM,
    REAL_TIME_EVENTS,
}
enum class SharePermission { READ, WRITE, DELETE, SHARE, ADMIN }
enum class FederatedShareStatus { PENDING, ACCEPTED, DECLINED, REVOKED, EXPIRED }
enum class FederatedActivityType {
    SHARE_CREATED,
    SHARE_ACCEPTED,
    SHARE_DECLINED,
    FILE_ACCESSED,
    FILE_MODIFIED,
    COMMENT_ADDED,
    INSTANCE_ONLINE,
    INSTANCE_OFFLINE,
}

data class FederatedInstance(
    val id: String,
    val domain: String,
    val name: String,
    val description: String? = null,
    val version: String,
    val capabilities: List<FederationCapability>,
    val status: InstanceStatus,
    val lastSeenAt: Instant? = null,
    val registeredAt: Instant,
) {
    val isOnline: Boolean get() = status == InstanceStatus.ONLINE
}

data class FederatedShare(
    val id: String,
    val itemId: String,
    val sourceInstance: String,
    val targetInstance: String,
    val targetUserId: String? = null,
    val permissions: List<SharePermission>,
    val status: FederatedShareStatus,
    val expiresAt: Instant? = null,
    val createdBy: String,
    val createdAt: Instant,
    val acceptedAt: Instant? = null,
)

data class FederatedIdentity(
    val id: String,
    val localUserId: String? = null,
    val remoteUserId: String,
    val remoteInstance: String,
    val displayName: String,
    val email: String? = null,
    val avatarUrl: String? = null,
    val verified: Boolean,
    val linkedAt: Instant,
) {
    val federatedId: String get() = "$remoteUserId@$remoteInstance"
}

data class FederatedActivity(
    val id: String,
    val instanceDomain: String,
    val activityType: FederatedActivityType,
    val actorId: String,
    val objectId: String,
    val objectType: String,
    val summary: String,
    val timestamp: Instant,
)

/**
 * Request to create a federated share.
 */
data class CreateFederatedShareRequest(
    val itemId: String,
    val targetInstance: String,
    val targetUserId: String? = null,
    val permissions: List<SharePermission> = listOf(SharePermission.READ),
    val expiresInDays: Int? = null,
)

/**
 * Request to link a federated identity.
 */
data class LinkIdentityRequest(
    val remoteUserId: String,
    val remoteInstance: String,
    val displayName: String,
)
