/**
 * VaultStadio Advanced Search Dialog
 *
 * Dialog for advanced search with filters.
 */

package com.vaultstadio.app.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.metadata.model.AdvancedSearchRequest
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * File type filter options.
 */
enum class FileTypeFilter(val label: String, val mimeTypes: List<String>) {
    IMAGES("Images", listOf("image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml")),
    VIDEOS("Videos", listOf("video/mp4", "video/webm", "video/quicktime", "video/x-msvideo")),
    AUDIO("Audio", listOf("audio/mpeg", "audio/wav", "audio/ogg", "audio/flac")),
    DOCUMENTS(
        "Documents",
        listOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        ),
    ),
    SPREADSHEETS(
        "Spreadsheets",
        listOf("application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    ),
    PRESENTATIONS(
        "Presentations",
        listOf(
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        ),
    ),
    ARCHIVES(
        "Archives",
        listOf("application/zip", "application/x-rar-compressed", "application/x-7z-compressed", "application/gzip"),
    ),
    CODE(
        "Code",
        listOf("text/plain", "text/html", "text/css", "text/javascript", "application/json", "application/xml"),
    ),
}

/**
 * Date range filter options.
 */
enum class DateRangeFilter(val label: String, val days: Int?) {
    ANY("Any time", null),
    TODAY("Today", 1),
    WEEK("Past week", 7),
    MONTH("Past month", 30),
    QUARTER("Past 3 months", 90),
    YEAR("Past year", 365),
}

/**
 * Size range filter options.
 */
enum class SizeRangeFilter(val label: String, val minBytes: Long?, val maxBytes: Long?) {
    ANY("Any size", null, null),
    TINY("< 100 KB", null, 100 * 1024),
    SMALL("100 KB - 1 MB", 100 * 1024, 1024 * 1024),
    MEDIUM("1 MB - 10 MB", 1024 * 1024, 10 * 1024 * 1024),
    LARGE("10 MB - 100 MB", 10 * 1024 * 1024, 100L * 1024 * 1024),
    HUGE("> 100 MB", 100L * 1024 * 1024, null),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedSearchDialog(
    initialQuery: String = "",
    onSearch: (AdvancedSearchRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf(initialQuery) }
    var searchContent by remember { mutableStateOf(false) }
    var selectedFileTypes by remember { mutableStateOf<Set<FileTypeFilter>>(emptySet()) }
    var selectedDateRange by remember { mutableStateOf(DateRangeFilter.ANY) }
    var selectedSizeRange by remember { mutableStateOf(SizeRangeFilter.ANY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Advanced Search")
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Search query
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search query") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(Modifier.height(8.dp))

                // Search in content
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = searchContent,
                        onCheckedChange = { searchContent = it },
                    )
                    Text(
                        "Search in file content (full-text search)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // File types
                Text(
                    "File Types",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FileTypeFilter.entries.forEach { type ->
                        FilterChip(
                            selected = type in selectedFileTypes,
                            onClick = {
                                selectedFileTypes = if (type in selectedFileTypes) {
                                    selectedFileTypes - type
                                } else {
                                    selectedFileTypes + type
                                }
                            },
                            label = { Text(type.label) },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // Date range
                Text(
                    "Modified Date",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DateRangeFilter.entries.forEach { range ->
                        FilterChip(
                            selected = range == selectedDateRange,
                            onClick = { selectedDateRange = range },
                            label = { Text(range.label) },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // Size range
                Text(
                    "File Size",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SizeRangeFilter.entries.forEach { range ->
                        FilterChip(
                            selected = range == selectedSizeRange,
                            onClick = { selectedSizeRange = range },
                            label = { Text(range.label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fileTypes = if (selectedFileTypes.isEmpty()) {
                        null
                    } else {
                        selectedFileTypes.flatMap { it.mimeTypes }
                    }

                    val fromDate = selectedDateRange.days?.let { numDays ->
                        Clock.System.now().minus(numDays.days).toString()
                    }

                    onSearch(
                        AdvancedSearchRequest(
                            query = query,
                            searchContent = searchContent,
                            fileTypes = fileTypes,
                            minSize = selectedSizeRange.minBytes,
                            maxSize = selectedSizeRange.maxBytes,
                            fromDate = fromDate,
                            toDate = null,
                        ),
                    )
                    onDismiss()
                },
                enabled = query.isNotBlank(),
            ) {
                Icon(Icons.Default.Search, null)
                Spacer(Modifier.width(8.dp))
                Text("Search")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                // Reset all filters
                query = ""
                searchContent = false
                selectedFileTypes = emptySet()
                selectedDateRange = DateRangeFilter.ANY
                selectedSizeRange = SizeRangeFilter.ANY
            }) {
                Text("Reset")
            }
        },
    )
}
