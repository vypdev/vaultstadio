/**
 * VaultStadio Admin Components
 *
 * Reusable UI components for the Admin screen.
 */

package com.vaultstadio.app.ui.screens.admin

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.AdminUser
import com.vaultstadio.app.domain.auth.model.UserRole
import com.vaultstadio.app.domain.model.UserStatus
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import kotlinx.datetime.Clock
import org.jetbrains.compose.ui.tooling.preview.Preview

private val SampleAdminUser = AdminUser(
    id = "user-1",
    email = "john.doe@example.com",
    username = "johndoe",
    role = UserRole.USER,
    status = UserStatus.ACTIVE,
    avatarUrl = null,
    quotaBytes = 10L * 1024L * 1024L * 1024L, // 10 GB
    usedBytes = 3L * 1024L * 1024L * 1024L, // 3 GB
    createdAt = Clock.System.now(),
    lastLoginAt = Clock.System.now(),
)

/**
 * Card displaying user information.
 */
@Composable
fun UserCard(
    user: AdminUser,
    onEditQuota: () -> Unit,
    onEditRole: () -> Unit,
    onEditStatus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            UserHeader(
                user = user,
                onEditQuota = onEditQuota,
                onEditRole = onEditRole,
                onEditStatus = onEditStatus,
            )
            Spacer(modifier = Modifier.height(12.dp))
            StorageUsageBar(usedBytes = user.usedBytes, quotaBytes = user.quotaBytes)
        }
    }
}

@Composable
private fun UserHeader(
    user: AdminUser,
    onEditQuota: () -> Unit,
    onEditRole: () -> Unit,
    onEditStatus: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(user = user)
        Spacer(modifier = Modifier.width(12.dp))
        UserDetails(
            user = user,
            onEditRole = onEditRole,
            onEditStatus = onEditStatus,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onEditQuota) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit Quota",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun UserAvatar(user: AdminUser) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                when (user.role) {
                    UserRole.ADMIN -> MaterialTheme.colorScheme.error
                    UserRole.USER -> MaterialTheme.colorScheme.primary
                    UserRole.GUEST -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.surface
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = user.username.firstOrNull()?.uppercase() ?: "U",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun UserDetails(
    user: AdminUser,
    onEditRole: () -> Unit,
    onEditStatus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = user.username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.width(8.dp))
            RoleBadge(role = user.role, onClick = onEditRole)
            Spacer(modifier = Modifier.width(4.dp))
            StatusBadge(status = user.status, onClick = onEditStatus)
        }
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Role badge with color coding.
 */
@Composable
fun RoleBadge(
    role: UserRole,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val color = when (role) {
        UserRole.ADMIN -> MaterialTheme.colorScheme.error
        UserRole.USER -> MaterialTheme.colorScheme.primary
        UserRole.GUEST -> MaterialTheme.colorScheme.tertiary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = role.name,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

/**
 * Status badge with color coding.
 */
@Composable
fun StatusBadge(
    status: UserStatus,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val color = when (status) {
        UserStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        UserStatus.INACTIVE -> MaterialTheme.colorScheme.outline
        UserStatus.SUSPENDED -> MaterialTheme.colorScheme.error
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

/**
 * Storage usage bar with quota info.
 */
@Composable
fun StorageUsageBar(
    usedBytes: Long,
    quotaBytes: Long?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Storage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${formatBytes(usedBytes)} / ${quotaBytes?.let { formatBytes(it) } ?: "Unlimited"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        val progress = if (quotaBytes != null && quotaBytes > 0) {
            (usedBytes.toFloat() / quotaBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = when {
                progress >= 0.9f -> MaterialTheme.colorScheme.error
                progress >= 0.75f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

/**
 * Format bytes to human-readable string.
 */
fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024L * 1024L * 1024L -> {
            val value = bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0)
            "${(value * 10).toLong() / 10.0} TB"
        }
        bytes >= 1024L * 1024L * 1024L -> {
            val value = bytes / (1024.0 * 1024.0 * 1024.0)
            "${(value * 10).toLong() / 10.0} GB"
        }
        bytes >= 1024L * 1024L -> {
            val value = bytes / (1024.0 * 1024.0)
            "${(value * 10).toLong() / 10.0} MB"
        }
        bytes >= 1024L -> {
            val value = bytes / 1024.0
            "${(value * 10).toLong() / 10.0} KB"
        }
        else -> "$bytes B"
    }
}

// region Previews

@Preview
@Composable
internal fun UserCardPreview() {
    VaultStadioPreview {
        UserCard(
            user = SampleAdminUser,
            onEditQuota = {},
            onEditRole = {},
            onEditStatus = {},
        )
    }
}

@Preview
@Composable
internal fun RoleBadgePreview() {
    VaultStadioPreview {
        Column {
            RoleBadge(role = UserRole.ADMIN, onClick = {})
            RoleBadge(role = UserRole.USER, onClick = {})
            RoleBadge(role = UserRole.GUEST, onClick = {})
        }
    }
}

@Preview
@Composable
internal fun StatusBadgePreview() {
    VaultStadioPreview {
        Column {
            StatusBadge(status = UserStatus.ACTIVE, onClick = {})
            StatusBadge(status = UserStatus.INACTIVE, onClick = {})
            StatusBadge(status = UserStatus.SUSPENDED, onClick = {})
        }
    }
}

@Preview
@Composable
internal fun StorageUsageBarPreview() {
    VaultStadioPreview {
        Column {
            StorageUsageBar(
                usedBytes = 3L * 1024L * 1024L * 1024L,
                quotaBytes = 10L * 1024L * 1024L * 1024L,
            )
            StorageUsageBar(
                usedBytes = 9L * 1024L * 1024L * 1024L,
                quotaBytes = 10L * 1024L * 1024L * 1024L,
            )
            StorageUsageBar(
                usedBytes = 5L * 1024L * 1024L * 1024L,
                quotaBytes = null,
            )
        }
    }
}

// endregion
