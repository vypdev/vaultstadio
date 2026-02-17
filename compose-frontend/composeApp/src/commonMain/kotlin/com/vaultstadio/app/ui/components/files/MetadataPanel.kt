/**
 * VaultStadio Metadata Panel
 *
 * Component for displaying file metadata (image, video, document).
 */

package com.vaultstadio.app.ui.components.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.DocumentMetadata
import com.vaultstadio.app.domain.model.ImageMetadata
import com.vaultstadio.app.domain.model.VideoMetadata
import com.vaultstadio.app.utils.formatFileSize

@Composable
fun MetadataPanel(
    imageMetadata: ImageMetadata? = null,
    videoMetadata: VideoMetadata? = null,
    documentMetadata: DocumentMetadata? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        imageMetadata?.let { ImageMetadataSection(it) }
        videoMetadata?.let { VideoMetadataSection(it) }
        documentMetadata?.let { DocumentMetadataSection(it) }
    }
}

@Composable
private fun ImageMetadataSection(metadata: ImageMetadata) {
    var expanded by remember { mutableStateOf(true) }

    MetadataCard(
        title = "Image Details",
        icon = Icons.Default.Image,
        expanded = expanded,
        onToggle = { expanded = !expanded },
    ) {
        // Basic info
        metadata.resolution?.let { MetadataRow("Resolution", it) }
        metadata.colorSpace?.let { MetadataRow("Color Space", it) }
        metadata.bitDepth?.let { MetadataRow("Bit Depth", "$it bit") }

        // Camera info
        if (metadata.cameraMake != null || metadata.cameraModel != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Camera",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))

            val camera = listOfNotNull(metadata.cameraMake, metadata.cameraModel).joinToString(" ")
            if (camera.isNotBlank()) MetadataRow("Camera", camera)
            metadata.aperture?.let { MetadataRow("Aperture", it) }
            metadata.exposureTime?.let { MetadataRow("Exposure", it) }
            metadata.iso?.let { MetadataRow("ISO", it.toString()) }
            metadata.focalLength?.let { MetadataRow("Focal Length", it) }
        }

        // Location
        if (metadata.hasLocation) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Location",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(4.dp))
            MetadataRow("Coordinates", "${metadata.gpsLatitude}, ${metadata.gpsLongitude}")
            metadata.gpsAltitude?.let { MetadataRow("Altitude", "${it}m") }
        }

        // IPTC/XMP
        if (metadata.keywords.isNotEmpty() || metadata.description != null) {
            Spacer(Modifier.height(8.dp))
            metadata.description?.let { MetadataRow("Description", it) }
            if (metadata.keywords.isNotEmpty()) {
                MetadataRow("Keywords", metadata.keywords.joinToString(", "))
            }
            metadata.artist?.let { MetadataRow("Artist", it) }
            metadata.copyright?.let { MetadataRow("Copyright", it) }
        }
    }
}

@Composable
private fun VideoMetadataSection(metadata: VideoMetadata) {
    var expanded by remember { mutableStateOf(true) }

    MetadataCard(
        title = "Video Details",
        icon = Icons.Default.Movie,
        expanded = expanded,
        onToggle = { expanded = !expanded },
    ) {
        // Basic info
        metadata.resolution?.let { MetadataRow("Resolution", it) }
        metadata.durationFormatted?.let { MetadataRow("Duration", it) }
        metadata.frameRate?.let { MetadataRow("Frame Rate", "$it fps") }
        metadata.bitrate?.let { MetadataRow("Bitrate", formatFileSize(it) + "/s") }
        metadata.aspectRatio?.let { MetadataRow("Aspect Ratio", it) }

        // Codec info
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Codecs",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(4.dp))
        metadata.videoCodec?.let { MetadataRow("Video Codec", it) }
        metadata.audioCodec?.let { MetadataRow("Audio Codec", it) }
        metadata.colorSpace?.let { MetadataRow("Color Space", it) }
        if (metadata.isHDR) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    "HDR",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        // Audio info
        if (metadata.hasAudio) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Audio",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(4.dp))
            metadata.channels?.let { MetadataRow("Channels", it.toString()) }
            metadata.sampleRate?.let { MetadataRow("Sample Rate", "$it Hz") }
            if (metadata.audioLanguages.isNotEmpty()) {
                MetadataRow("Languages", metadata.audioLanguages.joinToString(", "))
            }
        }

        // Media info
        metadata.title?.let { MetadataRow("Title", it) }
        metadata.artist?.let { MetadataRow("Artist", it) }
        metadata.chapterCount?.let { MetadataRow("Chapters", it.toString()) }
        if (metadata.subtitleTracks.isNotEmpty()) {
            MetadataRow("Subtitles", metadata.subtitleTracks.joinToString(", "))
        }
    }
}

@Composable
private fun DocumentMetadataSection(metadata: DocumentMetadata) {
    var expanded by remember { mutableStateOf(true) }

    MetadataCard(
        title = "Document Details",
        icon = Icons.Default.Description,
        expanded = expanded,
        onToggle = { expanded = !expanded },
    ) {
        metadata.title?.let { MetadataRow("Title", it) }
        metadata.author?.let { MetadataRow("Author", it) }
        metadata.subject?.let { MetadataRow("Subject", it) }
        metadata.pageCount?.let { MetadataRow("Pages", it.toString()) }
        metadata.wordCount?.let { MetadataRow("Words", it.toString()) }

        if (metadata.keywords.isNotEmpty()) {
            MetadataRow("Keywords", metadata.keywords.joinToString(", "))
        }

        metadata.creator?.let { MetadataRow("Creator", it) }
        metadata.producer?.let { MetadataRow("Producer", it) }

        if (metadata.isIndexed) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    "Full-text indexed",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun MetadataCard(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
                IconButton(onClick = onToggle, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
