/**
 * Federation DTO to domain mappers.
 */

package com.vaultstadio.app.data.federation.mapper

import com.vaultstadio.app.data.federation.dto.FederatedActivityDTO
import com.vaultstadio.app.data.federation.dto.FederatedIdentityDTO
import com.vaultstadio.app.data.federation.dto.FederatedInstanceDTO
import com.vaultstadio.app.data.federation.dto.FederatedShareDTO
import com.vaultstadio.app.domain.federation.model.FederatedActivity
import com.vaultstadio.app.domain.federation.model.FederatedActivityType
import com.vaultstadio.app.domain.federation.model.FederatedIdentity
import com.vaultstadio.app.domain.federation.model.FederatedInstance
import com.vaultstadio.app.domain.federation.model.FederatedShare
import com.vaultstadio.app.domain.federation.model.FederatedShareStatus
import com.vaultstadio.app.domain.federation.model.FederationCapability
import com.vaultstadio.app.domain.federation.model.InstanceStatus
import com.vaultstadio.app.domain.federation.model.SharePermission

fun FederatedInstanceDTO.toDomain(): FederatedInstance = FederatedInstance(
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

fun FederatedShareDTO.toDomain(): FederatedShare = FederatedShare(
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

fun FederatedIdentityDTO.toDomain(): FederatedIdentity = FederatedIdentity(
    id, localUserId, remoteUserId, remoteInstance, displayName, email, avatarUrl, verified, linkedAt,
)

fun FederatedActivityDTO.toDomain(): FederatedActivity = FederatedActivity(
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

fun List<FederatedInstanceDTO>.toInstanceList(): List<FederatedInstance> = map { it.toDomain() }
fun List<FederatedShareDTO>.toFederatedShareList(): List<FederatedShare> = map { it.toDomain() }
fun List<FederatedIdentityDTO>.toIdentityList(): List<FederatedIdentity> = map { it.toDomain() }
fun List<FederatedActivityDTO>.toFederatedActivityList(): List<FederatedActivity> = map { it.toDomain() }
