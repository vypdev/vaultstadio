/**
 * VaultStadio Sort Dialog
 *
 * Dialog for sorting files by different criteria.
 */

package com.vaultstadio.app.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.vaultstadio.app.domain.model.SortField
import com.vaultstadio.app.domain.model.SortOrder
import com.vaultstadio.app.i18n.strings

/**
 * Dialog for selecting sort field and order.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortDialog(
    currentSortField: SortField,
    currentSortOrder: SortOrder,
    onSortSelected: (SortField, SortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = strings()
    var selectedField by remember { mutableStateOf(currentSortField) }
    var selectedOrder by remember { mutableStateOf(currentSortOrder) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Sort by",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.padding(8.dp))

                // Sort fields
                SortField.entries.forEach { field ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedField = field }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedField == field,
                            onClick = { selectedField = field },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when (field) {
                                SortField.NAME -> "Name"
                                SortField.SIZE -> "Size"
                                SortField.CREATED_AT -> "Date created"
                                SortField.UPDATED_AT -> "Date modified"
                                SortField.TYPE -> "Type"
                            },
                        )
                    }
                }

                // Divider
                Spacer(Modifier.padding(vertical = 8.dp))
                Text(
                    "Order",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.padding(vertical = 4.dp))

                // Sort order
                SortOrder.entries.forEach { order ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOrder = order }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedOrder == order,
                            onClick = { selectedOrder = order },
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = if (order ==
                                SortOrder.ASC
                            ) {
                                Icons.Default.ArrowUpward
                            } else {
                                Icons.Default.ArrowDownward
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (order == SortOrder.ASC) "Ascending" else "Descending",
                        )
                    }
                }

                // Actions
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text(strings.actionCancel)
                    }
                    TextButton(
                        onClick = {
                            onSortSelected(selectedField, selectedOrder)
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
