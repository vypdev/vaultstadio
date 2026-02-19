/**
 * VaultStadio Admin Dialogs
 *
 * Dialog components for the Admin screen.
 */

package com.vaultstadio.app.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.AdminUser
import com.vaultstadio.app.domain.model.UserRole
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
 * Quota unit enum.
 */
enum class QuotaUnit(val label: String, val multiplier: Long) {
    MB("MB", 1024L * 1024L),
    GB("GB", 1024L * 1024L * 1024L),
    TB("TB", 1024L * 1024L * 1024L * 1024L),
}

/**
 * Dialog for editing user quota.
 */
@Composable
fun QuotaEditDialog(
    user: AdminUser,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit,
) {
    var quotaValue by remember {
        mutableStateOf(user.quotaBytes?.let { it / (1024 * 1024 * 1024) }?.toString() ?: "")
    }
    var selectedUnit by remember { mutableStateOf(QuotaUnit.GB) }
    var isUnlimited by remember { mutableStateOf(user.quotaBytes == null) }
    var showUnitMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Storage Quota") },
        text = {
            Column {
                Text(
                    text = "User: ${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Current usage: ${formatBytes(user.usedBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isUnlimited = !isUnlimited }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = isUnlimited, onCheckedChange = { isUnlimited = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlimited storage")
                }

                if (!isUnlimited) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = quotaValue,
                            onValueChange = { quotaValue = it.filter { c -> c.isDigit() } },
                            label = { Text("Quota") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box {
                            OutlinedButton(onClick = { showUnitMenu = true }) {
                                Text(selectedUnit.label)
                            }
                            DropdownMenu(
                                expanded = showUnitMenu,
                                onDismissRequest = { showUnitMenu = false },
                            ) {
                                QuotaUnit.entries.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit.label) },
                                        onClick = {
                                            selectedUnit = unit
                                            showUnitMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val quotaBytes = if (isUnlimited) {
                        null
                    } else {
                        quotaValue.toLongOrNull()?.times(selectedUnit.multiplier)
                    }
                    onConfirm(quotaBytes)
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * Dialog for editing user role.
 */
@Composable
fun RoleEditDialog(
    user: AdminUser,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var selectedRole by remember { mutableStateOf(user.role) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change User Role") },
        text = {
            Column {
                Text(
                    text = "User: ${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                UserRole.entries.forEach { role ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRole = role }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(role.name, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                            Text(
                                getRoleDescription(role),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedRole.name) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun getRoleDescription(role: UserRole): String = when (role) {
    UserRole.ADMIN -> "Full system access"
    UserRole.USER -> "Standard user access"
    UserRole.GUEST -> "Limited read-only access"
}

/**
 * Dialog for editing user status.
 */
@Composable
fun StatusEditDialog(
    user: AdminUser,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var selectedStatus by remember { mutableStateOf(user.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change User Status") },
        text = {
            Column {
                Text(
                    text = "User: ${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                UserStatus.entries.forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStatus = status }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(status.name, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                            Text(
                                getStatusDescription(status),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedStatus.name) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun getStatusDescription(status: UserStatus): String = when (status) {
    UserStatus.ACTIVE -> "User can access the system"
    UserStatus.INACTIVE -> "User account is disabled"
    UserStatus.SUSPENDED -> "User is temporarily blocked"
}

/**
 * Error dialog for admin operations.
 */
@Composable
fun AdminErrorDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}

// region Previews

@Preview
@Composable
internal fun QuotaEditDialogPreview() {
    VaultStadioPreview {
        QuotaEditDialog(
            user = SampleAdminUser,
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview
@Composable
internal fun RoleEditDialogPreview() {
    VaultStadioPreview {
        RoleEditDialog(
            user = SampleAdminUser,
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview
@Composable
internal fun StatusEditDialogPreview() {
    VaultStadioPreview {
        StatusEditDialog(
            user = SampleAdminUser,
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview
@Composable
internal fun AdminErrorDialogPreview() {
    VaultStadioPreview {
        AdminErrorDialog(
            message = "Failed to update user quota. Please try again.",
            onDismiss = {},
        )
    }
}

// endregion
