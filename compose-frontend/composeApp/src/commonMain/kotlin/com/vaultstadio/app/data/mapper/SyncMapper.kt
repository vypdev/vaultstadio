/**
 * Sync Mappers
 */

package com.vaultstadio.app.data.mapper

import com.vaultstadio.app.data.dto.sync.SyncChangeDTO
import com.vaultstadio.app.data.dto.sync.SyncConflictDTO
import com.vaultstadio.app.data.dto.sync.SyncDeviceDTO
import com.vaultstadio.app.data.dto.sync.SyncResponseDTO
import com.vaultstadio.app.domain.model.ChangeType
import com.vaultstadio.app.domain.model.ConflictType
import com.vaultstadio.app.domain.model.DeviceType
import com.vaultstadio.app.domain.model.SyncChange
import com.vaultstadio.app.domain.model.SyncConflict
import com.vaultstadio.app.domain.model.SyncDevice
import com.vaultstadio.app.domain.model.SyncResponse

fun SyncDeviceDTO.toDomain(): SyncDevice = SyncDevice(
    id = id,
    deviceId = deviceId,
    deviceName = deviceName,
    deviceType = try {
        DeviceType.valueOf(deviceType)
    } catch (e: Exception) {
        DeviceType.OTHER
    },
    lastSyncAt = lastSyncAt,
    isActive = isActive,
    createdAt = createdAt,
)

fun SyncChangeDTO.toDomain(): SyncChange = SyncChange(
    id = id,
    itemId = itemId,
    changeType = try {
        ChangeType.valueOf(changeType)
    } catch (e: Exception) {
        ChangeType.MODIFY
    },
    timestamp = timestamp,
    cursor = cursor,
    oldPath = oldPath,
    newPath = newPath,
    checksum = checksum,
)

fun SyncConflictDTO.toDomain(): SyncConflict = SyncConflict(
    id = id,
    itemId = itemId,
    conflictType = try {
        ConflictType.valueOf(conflictType)
    } catch (e: Exception) {
        ConflictType.EDIT_CONFLICT
    },
    localChange = localChange.toDomain(),
    remoteChange = remoteChange.toDomain(),
    createdAt = createdAt,
    isPending = isPending,
)

fun SyncResponseDTO.toDomain(): SyncResponse = SyncResponse(
    changes = changes.map { it.toDomain() },
    cursor = cursor,
    hasMore = hasMore,
    conflicts = conflicts.map { it.toDomain() },
    serverTime = serverTime,
)

fun List<SyncDeviceDTO>.toDeviceList(): List<SyncDevice> = map { it.toDomain() }
fun List<SyncConflictDTO>.toConflictList(): List<SyncConflict> = map { it.toDomain() }
