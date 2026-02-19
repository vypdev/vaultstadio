/**
 * ResultRow mappers for sync entities.
 * Extracted from ExposedSyncRepository to keep the main file under the line limit.
 */

package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.core.domain.model.ChangeType
import com.vaultstadio.core.domain.model.ConflictResolution
import com.vaultstadio.core.domain.model.ConflictType
import com.vaultstadio.core.domain.model.DeviceType
import com.vaultstadio.core.domain.model.SyncChange
import com.vaultstadio.core.domain.model.SyncConflict
import com.vaultstadio.core.domain.model.SyncDevice
import com.vaultstadio.infrastructure.persistence.entities.SyncChangesTable
import com.vaultstadio.infrastructure.persistence.entities.SyncConflictsTable
import com.vaultstadio.infrastructure.persistence.entities.SyncDevicesTable
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow

private val json = Json { ignoreUnknownKeys = true }

internal fun ResultRow.toSyncDevice(): SyncDevice = SyncDevice(
    id = this[SyncDevicesTable.id],
    userId = this[SyncDevicesTable.userId],
    deviceId = this[SyncDevicesTable.deviceId],
    deviceName = this[SyncDevicesTable.deviceName],
    deviceType = DeviceType.valueOf(this[SyncDevicesTable.deviceType]),
    lastSyncAt = this[SyncDevicesTable.lastSyncAt],
    lastSyncCursor = this[SyncDevicesTable.lastSyncCursor],
    isActive = this[SyncDevicesTable.isActive],
    createdAt = this[SyncDevicesTable.createdAt],
    updatedAt = this[SyncDevicesTable.updatedAt],
)

internal fun ResultRow.toSyncChange(): SyncChange {
    val metadataStr = this[SyncChangesTable.metadata]
    val metadata = if (metadataStr != null) {
        try {
            json.decodeFromString<Map<String, String>>(metadataStr)
        } catch (e: Exception) {
            emptyMap()
        }
    } else {
        emptyMap()
    }

    return SyncChange(
        id = this[SyncChangesTable.id],
        itemId = this[SyncChangesTable.itemId],
        changeType = ChangeType.valueOf(this[SyncChangesTable.changeType]),
        userId = this[SyncChangesTable.userId],
        deviceId = this[SyncChangesTable.deviceId],
        timestamp = this[SyncChangesTable.timestamp],
        cursor = this[SyncChangesTable.cursor],
        oldPath = this[SyncChangesTable.oldPath],
        newPath = this[SyncChangesTable.newPath],
        checksum = this[SyncChangesTable.checksum],
        metadata = metadata,
    )
}

internal fun ResultRow.toSyncConflict(
    localChange: SyncChange?,
    remoteChange: SyncChange?,
): SyncConflict {
    val dummyChange = SyncChange(
        id = "",
        itemId = this[SyncConflictsTable.itemId],
        changeType = ChangeType.MODIFY,
        userId = "",
        deviceId = null,
        timestamp = Clock.System.now(),
        cursor = 0,
    )

    return SyncConflict(
        id = this[SyncConflictsTable.id],
        itemId = this[SyncConflictsTable.itemId],
        localChange = localChange ?: dummyChange,
        remoteChange = remoteChange ?: dummyChange,
        conflictType = ConflictType.valueOf(this[SyncConflictsTable.conflictType]),
        resolvedAt = this[SyncConflictsTable.resolvedAt],
        resolution = this[SyncConflictsTable.resolution]?.let { ConflictResolution.valueOf(it) },
        createdAt = this[SyncConflictsTable.createdAt],
    )
}
