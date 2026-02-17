/**
 * VaultStadio Security Models
 *
 * Models for security-related features.
 */

package com.vaultstadio.app.domain.model

import kotlinx.datetime.Instant

/**
 * Represents an active session/device.
 */
data class ActiveSession(
    val id: String,
    val deviceName: String,
    val deviceType: SessionDeviceType,
    val lastActiveAt: Instant,
    val location: String?,
    val ipAddress: String?,
    val isCurrent: Boolean,
)

/**
 * Device type for sessions.
 */
enum class SessionDeviceType {
    WEB,
    MOBILE,
    DESKTOP,
    UNKNOWN,
}

/**
 * Represents a login event in the history.
 */
data class LoginEvent(
    val id: String,
    val timestamp: Instant,
    val ipAddress: String?,
    val location: String?,
    val deviceInfo: String?,
    val success: Boolean,
)

/**
 * Security settings for the user.
 */
data class SecuritySettings(
    val twoFactorEnabled: Boolean,
    val twoFactorMethod: TwoFactorMethod?,
)

/**
 * Two-factor authentication method.
 */
enum class TwoFactorMethod {
    TOTP,
    SMS,
    EMAIL,
}
