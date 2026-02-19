/**
 * VaultStadio Storage Chart
 *
 * Visual representation of storage usage by file type.
 */

package com.vaultstadio.app.ui.components.files

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.core.resources.strings
import com.vaultstadio.app.utils.formatFileSize

/**
 * Storage category for the chart.
 */
data class StorageCategory(
    val name: String,
    val bytes: Long,
    val color: Color,
)

/**
 * Storage usage chart card.
 */
@Composable
fun StorageChartCard(
    usedBytes: Long,
    totalBytes: Long?,
    categories: List<StorageCategory>,
    modifier: Modifier = Modifier,
) {
    val strings = strings()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Title
            Text(
                strings.settingsStorage,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(16.dp))

            // Donut chart and summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Donut chart
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    StorageDonutChart(
                        categories = categories,
                        totalBytes = usedBytes,
                        modifier = Modifier.size(100.dp),
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            formatFileSize(usedBytes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            strings.storageUsed,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                // Categories legend
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categories.forEach { category ->
                        StorageCategoryItem(category = category)
                    }
                }
            }

            // Total progress bar
            if (totalBytes != null && totalBytes > 0) {
                Spacer(Modifier.height(16.dp))

                val progress = (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                val progressColor = when {
                    progress >= 0.9f -> MaterialTheme.colorScheme.error
                    progress >= 0.75f -> Color(0xFFFFA726)
                    else -> MaterialTheme.colorScheme.primary
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${formatFileSize(usedBytes)} / ${formatFileSize(totalBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = progressColor,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageDonutChart(
    categories: List<StorageCategory>,
    totalBytes: Long,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 20.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val canvasCenter = Offset(size.width / 2, size.height / 2)

        var startAngle = -90f

        categories.forEach { category ->
            val sweepAngle = if (totalBytes > 0) {
                (category.bytes.toFloat() / totalBytes.toFloat()) * 360f
            } else {
                0f
            }

            drawArc(
                color = category.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                topLeft = Offset(
                    x = canvasCenter.x - radius,
                    y = canvasCenter.y - radius,
                ),
                size = Size(radius * 2, radius * 2),
            )

            startAngle += sweepAngle
        }
    }
}

@Composable
private fun StorageCategoryItem(category: StorageCategory) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(category.color),
        )

        Spacer(Modifier.width(8.dp))

        Text(
            category.name,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )

        Text(
            formatFileSize(category.bytes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Storage warning banner when quota is almost full.
 */
@Composable
fun StorageWarningBanner(
    usedBytes: Long,
    totalBytes: Long,
    onManageStorage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)

    if (progress >= 0.85f) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (progress >= 0.95f) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    Color(0xFFFFF3E0)
                },
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (progress >= 0.95f) "Storage almost full" else "Storage running low",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (progress >= 0.95f) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            Color(0xFFE65100)
                        },
                    )
                    Text(
                        "${(progress * 100).toInt()}% used - ${formatFileSize(totalBytes - usedBytes)} remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (progress >= 0.95f) {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        } else {
                            Color(0xFFE65100).copy(alpha = 0.7f)
                        },
                    )
                }

                TextButton(onClick = onManageStorage) {
                    Text(
                        "Manage",
                        color = if (progress >= 0.95f) {
                            MaterialTheme.colorScheme.error
                        } else {
                            Color(0xFFE65100)
                        },
                    )
                }
            }
        }
    }
}
