/**
 * Federation Mappers
 */

package com.vaultstadio.app.data.mapper

import com.vaultstadio.app.data.dto.federation.FederatedActivityDTO
import com.vaultstadio.app.data.dto.federation.FederatedIdentityDTO
import com.vaultstadio.app.data.dto.federation.FederatedInstanceDTO
import com.vaultstadio.app.data.dto.federation.FederatedShareDTO
import com.vaultstadio.app.domain.model.FederatedActivity
import com.vaultstadio.app.domain.model.FederatedActivityType
import com.vaultstadio.app.domain.model.FederatedIdentity
import com.vaultstadio.app.domain.model.FederatedInstance
import com.vaultstadio.app.domain.model.FederatedShare
import com.vaultstadio.app.domain.model.FederatedShareStatus
import com.vaultstadio.app.domain.model.FederationCapability
import com.vaultstadio.app.domain.model.InstanceStatus
import com.vaultstadio.app.domain.model.SharePermission

fun FederatedInstanceDTO.toDomain() = FederatedInstance(
    id, domain, name, description, version,
    capabilities.mapNotNull {
        try {
            FederationCapability.valueOf(it)
        } catch (e: Exception) {
            null
        }
    },
    try {
        InstanceStatus.valueOf(status)
    } catch (e: Exception) {
        InstanceStatus.OFFLINE
    },
    lastSeenAt, registeredAt,
)

fun FederatedShareDTO.toDomain() = FederatedShare(
    id, itemId, sourceInstance, targetInstance, targetUserId,
    permissions.mapNotNull {
        try {
            SharePermission.valueOf(it)
        } catch (e: Exception) {
            null
        }
    },
    try {
        FederatedShareStatus.valueOf(status)
    } catch (e: Exception) {
        FederatedShareStatus.PENDING
    },
    expiresAt, createdBy, createdAt, acceptedAt,
)

fun FederatedIdentityDTO.toDomain() = FederatedIdentity(
    id, localUserId, remoteUserId, remoteInstance, displayName, email, avatarUrl, verified, linkedAt,
)

fun FederatedActivityDTO.toDomain() = FederatedActivity(
    id,
    instanceDomain,
    try {
        FederatedActivityType.valueOf(activityType)
    } catch (e: Exception) {
        FederatedActivityType.FILE_ACCESSED
    },
    actorId,
    objectId,
    objectType,
    summary,
    timestamp,
)

fun List<FederatedInstanceDTO>.toInstanceList() = map { it.toDomain() }
fun List<FederatedShareDTO>.toFederatedShareList() = map { it.toDomain() }
fun List<FederatedIdentityDTO>.toIdentityList() = map { it.toDomain() }
fun List<FederatedActivityDTO>.toFederatedActivityList() = map { it.toDomain() }
