/**
 * ResultRow mappers for federation entities.
 * Extracted from ExposedFederationRepository to keep the main file under the line limit.
 */

package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.core.domain.model.FederatedActivity
import com.vaultstadio.core.domain.model.FederatedActivityType
import com.vaultstadio.core.domain.model.FederatedIdentity
import com.vaultstadio.core.domain.model.FederatedInstance
import com.vaultstadio.core.domain.model.FederatedShare
import com.vaultstadio.core.domain.model.FederatedShareStatus
import com.vaultstadio.core.domain.model.FederationCapability
import com.vaultstadio.core.domain.model.InstanceStatus
import com.vaultstadio.core.domain.model.SharePermission
import com.vaultstadio.infrastructure.persistence.entities.FederatedActivitiesTable
import com.vaultstadio.infrastructure.persistence.entities.FederatedIdentitiesTable
import com.vaultstadio.infrastructure.persistence.entities.FederatedInstancesTable
import com.vaultstadio.infrastructure.persistence.entities.FederatedSharesTable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow

private val json = Json { ignoreUnknownKeys = true }

internal fun ResultRow.toFederatedInstance(): FederatedInstance {
    val capabilitiesStr = this[FederatedInstancesTable.capabilities]
    val capabilities = try {
        json.decodeFromString<List<String>>(capabilitiesStr)
            .mapNotNull {
                try {
                    FederationCapability.valueOf(it)
                } catch (e: Exception) {
                    null
                }
            }
    } catch (e: Exception) {
        emptyList()
    }

    val metadataStr = this[FederatedInstancesTable.metadata]
    val metadata = if (metadataStr != null) {
        try {
            json.decodeFromString<Map<String, String>>(metadataStr)
        } catch (e: Exception) {
            emptyMap()
        }
    } else {
        emptyMap()
    }

    return FederatedInstance(
        id = this[FederatedInstancesTable.id],
        domain = this[FederatedInstancesTable.domain],
        name = this[FederatedInstancesTable.name],
        description = this[FederatedInstancesTable.description],
        version = this[FederatedInstancesTable.version],
        publicKey = this[FederatedInstancesTable.publicKey],
        capabilities = capabilities,
        status = InstanceStatus.valueOf(this[FederatedInstancesTable.status]),
        lastSeenAt = this[FederatedInstancesTable.lastSeenAt],
        registeredAt = this[FederatedInstancesTable.registeredAt],
        metadata = metadata,
    )
}

internal fun ResultRow.toFederatedShare(): FederatedShare {
    val permissionsStr = this[FederatedSharesTable.permissions]
    val permissions = try {
        json.decodeFromString<List<String>>(permissionsStr)
            .mapNotNull {
                try {
                    SharePermission.valueOf(it)
                } catch (e: Exception) {
                    null
                }
            }
    } catch (e: Exception) {
        emptyList()
    }

    return FederatedShare(
        id = this[FederatedSharesTable.id],
        itemId = this[FederatedSharesTable.itemId],
        sourceInstance = this[FederatedSharesTable.sourceInstance],
        targetInstance = this[FederatedSharesTable.targetInstance],
        targetUserId = this[FederatedSharesTable.targetUserId],
        permissions = permissions,
        expiresAt = this[FederatedSharesTable.expiresAt],
        createdBy = this[FederatedSharesTable.createdBy],
        createdAt = this[FederatedSharesTable.createdAt],
        acceptedAt = this[FederatedSharesTable.acceptedAt],
        status = FederatedShareStatus.valueOf(this[FederatedSharesTable.status]),
    )
}

internal fun ResultRow.toFederatedIdentity(): FederatedIdentity = FederatedIdentity(
    id = this[FederatedIdentitiesTable.id],
    localUserId = this[FederatedIdentitiesTable.localUserId],
    remoteUserId = this[FederatedIdentitiesTable.remoteUserId],
    remoteInstance = this[FederatedIdentitiesTable.remoteInstance],
    displayName = this[FederatedIdentitiesTable.displayName],
    email = this[FederatedIdentitiesTable.email],
    avatarUrl = this[FederatedIdentitiesTable.avatarUrl],
    verified = this[FederatedIdentitiesTable.verified],
    linkedAt = this[FederatedIdentitiesTable.linkedAt],
)

internal fun ResultRow.toFederatedActivity(): FederatedActivity {
    val metadataStr = this[FederatedActivitiesTable.metadata]
    val metadata = if (metadataStr != null) {
        try {
            json.decodeFromString<Map<String, String>>(metadataStr)
        } catch (e: Exception) {
            emptyMap()
        }
    } else {
        emptyMap()
    }

    return FederatedActivity(
        id = this[FederatedActivitiesTable.id],
        instanceDomain = this[FederatedActivitiesTable.instanceDomain],
        activityType = FederatedActivityType.valueOf(this[FederatedActivitiesTable.activityType]),
        actorId = this[FederatedActivitiesTable.actorId],
        objectId = this[FederatedActivitiesTable.objectId],
        objectType = this[FederatedActivitiesTable.objectType],
        summary = this[FederatedActivitiesTable.summary],
        timestamp = this[FederatedActivitiesTable.timestamp],
        metadata = metadata,
    )
}
