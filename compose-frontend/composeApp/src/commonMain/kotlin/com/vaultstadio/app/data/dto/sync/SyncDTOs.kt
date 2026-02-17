/**
 * Sync Data Transfer Objects
 */

package com.vaultstadio.app.data.dto.sync

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SyncDeviceDTO(
    val id: String,
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    val lastSyncAt: Instant? = null,
    val isActive: Boolean,
    val createdAt: Instant,
)

@Serializable
data class SyncChangeDTO(
    val id: String,
    val itemId: String,
    val changeType: String,
    val timestamp: Instant,
    val cursor: Long,
    val oldPath: String? = null,
    val newPath: String? = null,
    val checksum: String? = null,
)

@Serializable
data class SyncConflictDTO(
    val id: String,
    val itemId: String,
    val conflictType: String,
    val localChange: SyncChangeDTO,
    val remoteChange: SyncChangeDTO,
    val createdAt: Instant,
    val isPending: Boolean,
)

@Serializable
data class SyncRequestDTO(
    val cursor: String? = null,
    val limit: Int = 1000,
    val includeDeleted: Boolean = true,
)

@Serializable
data class SyncResponseDTO(
    val changes: List<SyncChangeDTO>,
    val cursor: String,
    val hasMore: Boolean,
    val conflicts: List<SyncConflictDTO>,
    val serverTime: Instant,
)

@Serializable
data class RegisterDeviceRequestDTO(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
)
