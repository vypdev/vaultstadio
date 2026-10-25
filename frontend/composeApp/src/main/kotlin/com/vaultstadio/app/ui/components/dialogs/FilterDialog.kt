/**
 * VaultStadio Filter Dialog
 *
 * Dialog for filtering files by type, date, and size.
 * Uses shared filter enums from AdvancedSearchDialog.
 */

package com.vaultstadio.app.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.core.resources.strings

/**
 * Filter configuration.
 */
data class FilterConfig(
    val fileTypes: Set<String> = emptySet(),
    val dateRangeDays: Int? = null,
    val minSizeBytes: Long? = null,
    val maxSizeBytes: Long? = null,
    val searchInContent: Boolean = false,
)

/**
 * Dialog for filtering files.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    currentFilter: FilterConfig,
    onFilterApplied: (FilterConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = strings()
    var selectedTypes by remember { mutableStateOf(currentFilter.fileTypes) }
    var selectedDateDays by remember { mutableStateOf(currentFilter.dateRangeDays) }
    var selectedMinSize by remember { mutableStateOf(currentFilter.minSizeBytes) }
    var selectedMaxSize by remember { mutableStateOf(currentFilter.maxSizeBytes) }
    var searchInContent by remember { mutableStateOf(currentFilter.searchInContent) }

    // File type options
    val fileTypeOptions = listOf(
        "Images" to listOf("jpg", "png", "gif"),
        "Videos" to listOf("mp4", "mov", "avi"),
        "Audio" to listOf("mp3", "wav", "flac"),
        "Documents" to listOf("pdf", "doc", "txt"),
        "Folders" to emptyList(),
        "Other" to emptyList(),
    )

    // Date range options
    val dateRangeOptions = listOf(
        "Today" to 1,
        "Last 7 days" to 7,
        "Last 30 days" to 30,
        "Last year" to 365,
        "Any time" to null,
    )

    // Size range options
    val sizeRangeOptions = listOf(
        "< 1 MB" to (null to 1L * 1024 * 1024),
        "1 - 10 MB" to (1L * 1024 * 1024 to 10L * 1024 * 1024),
        "10 - 100 MB" to (10L * 1024 * 1024 to 100L * 1024 * 1024),
        "100 MB - 1 GB" to (100L * 1024 * 1024 to 1L * 1024 * 1024 * 1024),
        "> 1 GB" to (1L * 1024 * 1024 * 1024 to null),
        "Any size" to (null to null),
    )

    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "Filter",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(16.dp))

                // File types
                Text(
                    strings.searchFileType,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))

                fileTypeOptions.forEach { (type, _) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTypes = if (selectedTypes.contains(type)) {
                                    selectedTypes - type
                                } else {
                                    selectedTypes + type
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = selectedTypes.contains(type),
                            onCheckedChange = {
                                selectedTypes = if (it) selectedTypes + type else selectedTypes - type
                            },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(type)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Date range
                Text(
                    strings.searchDateRange,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    dateRangeOptions.take(3).forEach { (label, days) ->
                        FilterChipButton(
                            label = label,
                            selected = selectedDateDays == days,
                            onClick = { selectedDateDays = days },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    dateRangeOptions.drop(3).forEach { (label, days) ->
                        FilterChipButton(
                            label = label,
                            selected = selectedDateDays == days,
                            onClick = { selectedDateDays = days },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Size range
                Text(
                    strings.searchSizeRange,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    sizeRangeOptions.take(3).forEach { (label, range) ->
                        FilterChipButton(
                            label = label,
                            selected = selectedMinSize == range.first && selectedMaxSize == range.second,
                            onClick = {
                                selectedMinSize = range.first
                                selectedMaxSize = range.second
                            },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    sizeRangeOptions.drop(3).forEach { (label, range) ->
                        FilterChipButton(
                            label = label,
                            selected = selectedMinSize == range.first && selectedMaxSize == range.second,
                            onClick = {
                                selectedMinSize = range.first
                                selectedMaxSize = range.second
                            },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Search in content
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { searchInContent = !searchInContent }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = searchInContent,
                        onCheckedChange = { searchInContent = it },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(strings.searchByContent)
                }

                Spacer(Modifier.height(16.dp))

                // Actions
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            selectedTypes = emptySet()
                            selectedDateDays = null
                            selectedMinSize = null
                            selectedMaxSize = null
                            searchInContent = false
                        },
                    ) {
                        Text(strings.searchReset)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text(strings.actionCancel)
                    }
                    Button(
                        onClick = {
                            onFilterApplied(
                                FilterConfig(
                                    fileTypes = selectedTypes,
                                    dateRangeDays = selectedDateDays,
                                    minSizeBytes = selectedMinSize,
                                    maxSizeBytes = selectedMaxSize,
                                    searchInContent = searchInContent,
                                ),
                            )
                            onDismiss()
                        },
                    ) {
                        Text(strings.actionDone)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(
            onClick = onClick,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
