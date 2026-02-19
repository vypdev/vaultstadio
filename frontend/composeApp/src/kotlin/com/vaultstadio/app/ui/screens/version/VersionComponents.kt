/**
 * VaultStadio Version History Components
 *
 * Reusable UI components for the Version History screen.
 */

package com.vaultstadio.app.ui.screens.version

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.FileVersion
import com.vaultstadio.app.domain.model.VersionDiff
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import com.vaultstadio.app.utils.formatFileSize
import com.vaultstadio.app.utils.formatRelativeTime
import kotlin.time.Clock
import org.jetbrains.compose.ui.tooling.preview.Preview

private val SampleVersion = FileVersion(
    id = "version-1",
    itemId = "item-1",
    versionNumber = 1,
    size = 1024L * 1024L,
    createdAt = Clock.System.now(),
    createdBy = "john.doe",
    comment = "Initial version",
    checksum = "abc123",
    isLatest = true,
)

private val SampleDiff = VersionDiff(
    fromVersion = 1,
    toVersion = 2,
    sizeChange = 1024L,
    additions = 45,
    deletions = 12,
    isBinary = false,
)

/**
 * Summary item showing a statistic.
 */
@Composable
fun SummaryItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Card displaying a file version.
 */
@Composable
fun VersionCard(
    version: FileVersion,
    onRestore: () -> Unit,
    onDownload: () -> Unit,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit,
    onCompare: () -> Unit,
    hasPreviousVersion: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (version.isLatest) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            VersionHeader(version = version)

            version.comment?.let { comment ->
                Spacer(Modifier.height(8.dp))
                Text(
                    comment,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))

            VersionActions(
                version = version,
                hasPreviousVersion = hasPreviousVersion,
                onRestore = onRestore,
                onDownload = onDownload,
                onViewDetails = onViewDetails,
                onCompare = onCompare,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun VersionHeader(version: FileVersion) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VersionBadge(version = version)
            Spacer(Modifier.width(12.dp))
            VersionInfo(version = version)
        }
    }
}

@Composable
private fun VersionBadge(version: FileVersion) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (version.isLatest) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "v${version.versionNumber}",
            style = MaterialTheme.typography.labelMedium,
            color = if (version.isLatest) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun VersionInfo(version: FileVersion) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Version ${version.versionNumber}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            if (version.isLatest) {
                Spacer(Modifier.width(8.dp))
                VersionTag("CURRENT", MaterialTheme.colorScheme.primary)
            }
            if (version.isRestore) {
                Spacer(Modifier.width(8.dp))
                VersionTag(
                    "RESTORED FROM v${version.restoredFrom}",
                    MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        Text(
            "${formatRelativeTime(version.createdAt)} • ${formatFileSize(version.size)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VersionTag(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun VersionActions(
    version: FileVersion,
    hasPreviousVersion: Boolean,
    onRestore: () -> Unit,
    onDownload: () -> Unit,
    onViewDetails: () -> Unit,
    onCompare: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!version.isLatest) {
            ActionButton(Icons.Default.Restore, "Restore", onClick = onRestore)
        }
        ActionButton(Icons.Default.Download, "Download", onClick = onDownload)
        ActionButton(Icons.Default.Info, "Details", onClick = onViewDetails)
        if (hasPreviousVersion) {
            ActionButton(Icons.Default.Compare, "Compare", onClick = onCompare)
        }
        if (!version.isLatest) {
            ActionButton(
                Icons.Default.Delete,
                "Delete",
                onClick = onDelete,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = tint)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

/**
 * Diff statistics view.
 */
@Composable
fun DiffView(
    diff: VersionDiff,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DiffStat(
            icon = Icons.Default.Add,
            value = "+${diff.additions}",
            color = Color(0xFF4CAF50),
            label = "Additions",
        )
        DiffStat(
            icon = Icons.Default.Remove,
            value = "-${diff.deletions}",
            color = Color(0xFFE53935),
            label = "Deletions",
        )
    }
}

@Composable
private fun DiffStat(
    icon: ImageVector,
    value: String,
    color: Color,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(value, color = color, fontFamily = FontFamily.Monospace)
    }
}

/**
 * Empty state when no versions exist.
 */
@Composable
fun EmptyVersionState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No Version History",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Version history will appear here when you make changes to this file.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// region Previews

@Preview
@Composable
internal fun SummaryItemPreview() {
    VaultStadioPreview {
        SummaryItem(label = "Total Versions", value = "12")
    }
}

@Preview
@Composable
internal fun VersionCardPreview() {
    VaultStadioPreview {
        VersionCard(
            version = SampleVersion,
            onRestore = {},
            onDownload = {},
            onViewDetails = {},
            onCompare = {},
            onDelete = {},
            hasPreviousVersion = true,
        )
    }
}

@Preview
@Composable
internal fun DiffViewPreview() {
    VaultStadioPreview {
        DiffView(diff = SampleDiff)
    }
}

@Preview
@Composable
internal fun EmptyVersionStatePreview() {
    VaultStadioPreview {
        EmptyVersionState()
    }
}

// endregion
