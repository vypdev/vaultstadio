/**
 * Register device request DTO.
 */

package com.vaultstadio.app.data.sync.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequestDTO(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
)
