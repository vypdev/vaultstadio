/**
 * VaultStadio Profile Components
 *
 * Reusable UI components for the Profile screen.
 */

package com.vaultstadio.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.StorageQuota
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import org.jetbrains.compose.ui.tooling.preview.Preview

private val SampleStorageQuota = StorageQuota(
    usedBytes = 15L * 1024L * 1024L * 1024L, // 15 GB
    quotaBytes = 100L * 1024L * 1024L * 1024L, // 100 GB
    usagePercentage = 15.0,
    fileCount = 1234L,
    folderCount = 56L,
    remainingBytes = 85L * 1024L * 1024L * 1024L, // 85 GB
)

/**
 * Profile header with avatar, name, and membership info.
 */
@Composable
fun ProfileHeader(
    name: String,
    email: String,
    memberSince: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProfileAvatar(name = name)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            if (email.isNotBlank()) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (memberSince.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(
                    onClick = { },
                    label = { Text("Member since $memberSince") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .take(2)
                .joinToString(""),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Storage quota card with progress indicator.
 */
@Composable
fun StorageQuotaCard(
    usedBytes: Long,
    totalBytes: Long,
    usedPercentage: Float,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Storage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { (usedPercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = when {
                    usedPercentage > 90 -> MaterialTheme.colorScheme.error
                    usedPercentage > 75 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${formatBytes(usedBytes)} used",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (totalBytes > 0) {
                    Text(
                        text = "${formatBytes(totalBytes - usedBytes)} free",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (totalBytes > 0) {
                Text(
                    text = "${((usedPercentage * 10).toLong() / 10.0)}% of ${formatBytes(totalBytes)} used",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Unlimited storage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Quick stats card showing folder and file counts.
 */
@Composable
fun QuickStatsCard(
    quota: StorageQuota?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem(value = quota?.folderCount?.toString() ?: "-", label = "Folders")
            StatItem(value = quota?.fileCount?.toString() ?: "-", label = "Files")
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Account actions card with navigation items.
 */
@Composable
fun AccountActionsCard(
    onChangePassword: () -> Unit,
    onSecuritySettings: () -> Unit = {},
    onConnectedDevices: () -> Unit = {},
    onLoginHistory: () -> Unit = {},
    onExportData: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column {
            AccountActionItem(title = "Change Password", onClick = onChangePassword)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            AccountActionItem(title = "Security Settings", onClick = onSecuritySettings)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            AccountActionItem(title = "Connected Devices", onClick = onConnectedDevices)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            AccountActionItem(title = "Login History", onClick = onLoginHistory)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            AccountActionItem(title = "Export Data", onClick = onExportData)
        }
    }
}

@Composable
private fun AccountActionItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Format bytes to human-readable string.
 */
fun formatBytes(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0

    while (value >= 1024 && unitIndex < units.size - 1) {
        value /= 1024
        unitIndex++
    }

    return if (unitIndex == 0) {
        "${value.toLong()} ${units[unitIndex]}"
    } else {
        val rounded = ((value * 10).toLong() / 10.0)
        "$rounded ${units[unitIndex]}"
    }
}

/**
 * Get month name from month number.
 */
fun getMonthName(month: Int): String = when (month) {
    1 -> "January"
    2 -> "February"
    3 -> "March"
    4 -> "April"
    5 -> "May"
    6 -> "June"
    7 -> "July"
    8 -> "August"
    9 -> "September"
    10 -> "October"
    11 -> "November"
    12 -> "December"
    else -> ""
}

// region Previews

@Preview
@Composable
internal fun ProfileHeaderPreview() {
    VaultStadioPreview {
        ProfileHeader(
            name = "John Doe",
            email = "john.doe@example.com",
            memberSince = "January 2024",
        )
    }
}

@Preview
@Composable
internal fun StorageQuotaCardPreview() {
    VaultStadioPreview {
        StorageQuotaCard(
            usedBytes = SampleStorageQuota.usedBytes,
            totalBytes = SampleStorageQuota.quotaBytes ?: 0L,
            usedPercentage = SampleStorageQuota.usagePercentage.toFloat(),
        )
    }
}

@Preview
@Composable
internal fun QuickStatsCardPreview() {
    VaultStadioPreview {
        QuickStatsCard(quota = SampleStorageQuota)
    }
}

@Preview
@Composable
internal fun AccountActionsCardPreview() {
    VaultStadioPreview {
        AccountActionsCard(
            onChangePassword = {},
            onSecuritySettings = {},
            onConnectedDevices = {},
            onLoginHistory = {},
            onExportData = {},
        )
    }
}

// endregion
