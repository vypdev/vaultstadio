/**
 * Sync device DTO.
 */

package com.vaultstadio.app.data.sync.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SyncDeviceDTO(
    val id: String,
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    @kotlinx.serialization.Contextual
    val lastSyncAt: Instant? = null,
    val isActive: Boolean,
    @kotlinx.serialization.Contextual
    val createdAt: Instant,
)
