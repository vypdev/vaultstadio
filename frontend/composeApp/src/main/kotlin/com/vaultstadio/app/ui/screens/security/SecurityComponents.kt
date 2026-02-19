/**
 * VaultStadio Security Components
 *
 * Reusable UI components for the Security screen.
 */

package com.vaultstadio.app.ui.screens.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.auth.model.ActiveSession
import com.vaultstadio.app.domain.auth.model.LoginEvent
import com.vaultstadio.app.domain.auth.model.SessionDeviceType
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import com.vaultstadio.app.utils.formatRelativeTime
import kotlin.time.Clock
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private val SampleActiveSession = ActiveSession(
    id = "session-1",
    deviceName = "MacBook Pro",
    deviceType = SessionDeviceType.DESKTOP,
    lastActiveAt = Clock.System.now(),
    location = "New York, USA",
    ipAddress = "192.168.1.100",
    isCurrent = true,
)

private val SampleActiveSessionOther = ActiveSession(
    id = "session-2",
    deviceName = "iPhone 14",
    deviceType = SessionDeviceType.MOBILE,
    lastActiveAt = Clock.System.now().minus(2.hours),
    location = "San Francisco, USA",
    ipAddress = "192.168.1.101",
    isCurrent = false,
)

private val SampleLoginEvent = LoginEvent(
    id = "event-1",
    timestamp = Clock.System.now().minus(1.hours),
    ipAddress = "192.168.1.100",
    location = "New York, USA",
    deviceInfo = "Chrome on macOS",
    success = true,
)

private val SampleLoginEventFailed = LoginEvent(
    id = "event-2",
    timestamp = Clock.System.now().minus(1.days),
    ipAddress = "192.168.1.200",
    location = "Unknown",
    deviceInfo = "Unknown",
    success = false,
)

/**
 * Section header for security sections.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(vertical = 8.dp),
    )
}

/**
 * Card for two-factor authentication setting.
 */
@Composable
fun TwoFactorCard(
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Two-Factor Authentication", fontWeight = FontWeight.Medium)
                    Text(
                        if (enabled) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            Switch(checked = enabled, onCheckedChange = { onToggle() })
        }
    }
}

/**
 * Card displaying an active session.
 */
@Composable
fun SessionCard(
    session: ActiveSession,
    onRevoke: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (session.isCurrent) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    getDeviceIcon(session.deviceType),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(session.deviceName, fontWeight = FontWeight.Medium)
                        if (session.isCurrent) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Current",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(
                        "Last active: ${formatRelativeTime(session.lastActiveAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    session.location?.let { location ->
                        Text(
                            location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (!session.isCurrent) {
                TextButton(onClick = onRevoke) {
                    Text("Revoke", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/**
 * Card displaying a login event.
 */
@Composable
fun LoginEventCard(
    event: LoginEvent,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (event.success) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (event.success) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    if (event.success) "Successful login" else "Failed login attempt",
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    formatRelativeTime(event.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                event.location?.let { location ->
                    Text(
                        location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Empty state card.
 */
@Composable
fun EmptyStateCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun getDeviceIcon(type: SessionDeviceType): ImageVector = when (type) {
    SessionDeviceType.DESKTOP -> Icons.Default.Computer
    SessionDeviceType.MOBILE -> Icons.Default.PhoneAndroid
    SessionDeviceType.WEB -> Icons.Default.Web
    SessionDeviceType.UNKNOWN -> Icons.Default.Computer
}

// region Previews

@Preview
@Composable
internal fun SectionHeaderPreview() {
    VaultStadioPreview {
        SectionHeader(title = "Active Sessions")
    }
}

@Preview
@Composable
internal fun TwoFactorCardPreview() {
    VaultStadioPreview {
        TwoFactorCard(
            enabled = true,
            onToggle = {},
        )
    }
}

@Preview
@Composable
internal fun TwoFactorCardDisabledPreview() {
    VaultStadioPreview {
        TwoFactorCard(
            enabled = false,
            onToggle = {},
        )
    }
}

@Preview
@Composable
internal fun SessionCardPreview() {
    VaultStadioPreview {
        SessionCard(
            session = SampleActiveSession,
            onRevoke = {},
        )
    }
}

@Preview
@Composable
internal fun SessionCardOtherPreview() {
    VaultStadioPreview {
        SessionCard(
            session = SampleActiveSessionOther,
            onRevoke = {},
        )
    }
}

@Preview
@Composable
internal fun LoginEventCardPreview() {
    VaultStadioPreview {
        LoginEventCard(event = SampleLoginEvent)
    }
}

@Preview
@Composable
internal fun LoginEventCardFailedPreview() {
    VaultStadioPreview {
        LoginEventCard(event = SampleLoginEventFailed)
    }
}

@Preview
@Composable
internal fun EmptyStateCardPreview() {
    VaultStadioPreview {
        EmptyStateCard(message = "No active sessions")
    }
}

// endregion
