package com.vaultstadio.app.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.User
import com.vaultstadio.app.i18n.LocalStrings
import com.vaultstadio.app.i18n.activity
import com.vaultstadio.app.i18n.admin
import com.vaultstadio.app.i18n.administration
import com.vaultstadio.app.i18n.advanced
import com.vaultstadio.app.i18n.ai
import com.vaultstadio.app.i18n.allFiles
import com.vaultstadio.app.i18n.collaboration
import com.vaultstadio.app.i18n.federation
import com.vaultstadio.app.i18n.files
import com.vaultstadio.app.i18n.logout
import com.vaultstadio.app.i18n.myShares
import com.vaultstadio.app.i18n.plugins
import com.vaultstadio.app.i18n.profile
import com.vaultstadio.app.i18n.recent
import com.vaultstadio.app.i18n.settings
import com.vaultstadio.app.i18n.sharedWithMe
import com.vaultstadio.app.i18n.sharing
import com.vaultstadio.app.i18n.starred
import com.vaultstadio.app.i18n.sync
import com.vaultstadio.app.i18n.trash
import com.vaultstadio.app.navigation.MainDestination

/**
 * Main sidebar navigation component.
 */
@Composable
fun MainSidebar(
    currentDestination: MainDestination,
    isAdmin: Boolean,
    currentUser: User?,
    onNavigate: (MainDestination) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    Surface(
        modifier = modifier
            .width(250.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
        ) {
            // App title
            Text(
                text = "VaultStadio",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp),
            )

            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Core navigation
            SidebarSection(title = strings.files) {
                SidebarItem(
                    icon = Icons.Default.Storage,
                    label = strings.allFiles,
                    selected = currentDestination == MainDestination.FILES,
                    onClick = { onNavigate(MainDestination.FILES) },
                )
                SidebarItem(
                    icon = Icons.Default.AccessTime,
                    label = strings.recent,
                    selected = currentDestination == MainDestination.RECENT,
                    onClick = { onNavigate(MainDestination.RECENT) },
                )
                SidebarItem(
                    icon = Icons.Default.Star,
                    label = strings.starred,
                    selected = currentDestination == MainDestination.STARRED,
                    onClick = { onNavigate(MainDestination.STARRED) },
                )
                SidebarItem(
                    icon = Icons.Default.Delete,
                    label = strings.trash,
                    selected = currentDestination == MainDestination.TRASH,
                    onClick = { onNavigate(MainDestination.TRASH) },
                )
            }

            // Sharing
            SidebarSection(title = strings.sharing) {
                SidebarItem(
                    icon = Icons.Default.Share,
                    label = strings.myShares,
                    selected = currentDestination == MainDestination.SHARED,
                    onClick = { onNavigate(MainDestination.SHARED) },
                )
                SidebarItem(
                    icon = Icons.Default.FolderShared,
                    label = strings.sharedWithMe,
                    selected = currentDestination == MainDestination.SHARED_WITH_ME,
                    onClick = { onNavigate(MainDestination.SHARED_WITH_ME) },
                )
            }

            // Advanced features
            SidebarSection(title = strings.advanced) {
                SidebarItem(
                    icon = Icons.Default.Psychology,
                    label = strings.ai,
                    selected = currentDestination == MainDestination.AI,
                    onClick = { onNavigate(MainDestination.AI) },
                )
                SidebarItem(
                    icon = Icons.Default.CloudSync,
                    label = strings.sync,
                    selected = currentDestination == MainDestination.SYNC,
                    onClick = { onNavigate(MainDestination.SYNC) },
                )
                SidebarItem(
                    icon = Icons.Default.Public,
                    label = strings.federation,
                    selected = currentDestination == MainDestination.FEDERATION,
                    onClick = { onNavigate(MainDestination.FEDERATION) },
                )
                SidebarItem(
                    icon = Icons.Default.Group,
                    label = strings.collaboration,
                    selected = currentDestination == MainDestination.COLLABORATION,
                    onClick = { onNavigate(MainDestination.COLLABORATION) },
                )
            }

            // Admin section (only for admins)
            if (isAdmin) {
                SidebarSection(title = strings.administration) {
                    SidebarItem(
                        icon = Icons.Default.AdminPanelSettings,
                        label = strings.admin,
                        selected = currentDestination == MainDestination.ADMIN,
                        onClick = { onNavigate(MainDestination.ADMIN) },
                    )
                    SidebarItem(
                        icon = Icons.Default.Timeline,
                        label = strings.activity,
                        selected = currentDestination == MainDestination.ACTIVITY,
                        onClick = { onNavigate(MainDestination.ACTIVITY) },
                    )
                    SidebarItem(
                        icon = Icons.Default.Extension,
                        label = strings.plugins,
                        selected = currentDestination == MainDestination.PLUGINS,
                        onClick = { onNavigate(MainDestination.PLUGINS) },
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider()

            // User section
            SidebarItem(
                icon = Icons.Default.Person,
                label = strings.profile,
                selected = currentDestination == MainDestination.PROFILE,
                onClick = { onNavigate(MainDestination.PROFILE) },
            )
            SidebarItem(
                icon = Icons.Default.Settings,
                label = strings.settings,
                selected = currentDestination == MainDestination.SETTINGS,
                onClick = { onNavigate(MainDestination.SETTINGS) },
            )
            SidebarItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                label = strings.logout,
                selected = false,
                onClick = onLogout,
            )

            // User info
            if (currentUser != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = currentUser.username.firstOrNull()?.uppercase() ?: "?",
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = currentUser.username,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = currentUser.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        content()
    }
}

@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
