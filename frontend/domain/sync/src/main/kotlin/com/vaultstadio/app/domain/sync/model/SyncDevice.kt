/**
 * Registered sync device.
 */

package com.vaultstadio.app.domain.sync.model

import kotlinx.datetime.Instant

data class SyncDevice(
    val id: String,
    val deviceId: String,
    val deviceName: String,
    val deviceType: DeviceType,
    val lastSyncAt: Instant? = null,
    val isActive: Boolean,
    val createdAt: Instant,
)
