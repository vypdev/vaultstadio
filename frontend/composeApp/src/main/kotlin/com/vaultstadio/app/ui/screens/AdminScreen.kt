/**
 * VaultStadio Admin Screen
 *
 * Provides administrative functions for managing users and their quotas.
 */

package com.vaultstadio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.admin.model.AdminUser
import com.vaultstadio.app.core.resources.LocalStrings
import com.vaultstadio.app.ui.screens.admin.AdminErrorDialog
import com.vaultstadio.app.ui.screens.admin.QuotaEditDialog
import com.vaultstadio.app.ui.screens.admin.RoleEditDialog
import com.vaultstadio.app.ui.screens.admin.StatusEditDialog
import com.vaultstadio.app.ui.screens.admin.UserCard

/**
 * Admin screen for user management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    users: List<AdminUser>,
    isLoading: Boolean,
    error: String?,
    onNavigateBack: () -> Unit,
    onLoadUsers: () -> Unit,
    onUpdateQuota: (userId: String, quotaBytes: Long?) -> Unit,
    onUpdateUserRole: (userId: String, role: String) -> Unit,
    onUpdateUserStatus: (userId: String, status: String) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    var selectedUser by remember { mutableStateOf<AdminUser?>(null) }
    var showQuotaDialog by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onLoadUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.navMyFiles.substringBefore(" ").let { "User Management" }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        if (isLoading && users.isEmpty()) {
            Box(
                modifier = modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = "Users (${users.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                items(users) { user ->
                    UserCard(
                        user = user,
                        onEditQuota = {
                            selectedUser = user
                            showQuotaDialog = true
                        },
                        onEditRole = {
                            selectedUser = user
                            showRoleDialog = true
                        },
                        onEditStatus = {
                            selectedUser = user
                            showStatusDialog = true
                        },
                    )
                }
            }
        }
    }

    if (showQuotaDialog && selectedUser != null) {
        QuotaEditDialog(
            user = selectedUser!!,
            onDismiss = {
                showQuotaDialog = false
                selectedUser = null
            },
            onConfirm = { quotaBytes ->
                onUpdateQuota(selectedUser!!.id, quotaBytes)
                showQuotaDialog = false
                selectedUser = null
            },
        )
    }

    if (showRoleDialog && selectedUser != null) {
        RoleEditDialog(
            user = selectedUser!!,
            onDismiss = {
                showRoleDialog = false
                selectedUser = null
            },
            onConfirm = { roleName ->
                onUpdateUserRole(selectedUser!!.id, roleName)
                showRoleDialog = false
                selectedUser = null
            },
        )
    }

    if (showStatusDialog && selectedUser != null) {
        StatusEditDialog(
            user = selectedUser!!,
            onDismiss = {
                showStatusDialog = false
                selectedUser = null
            },
            onConfirm = { statusName ->
                onUpdateUserStatus(selectedUser!!.id, statusName)
                showStatusDialog = false
                selectedUser = null
            },
        )
    }

    error?.let { message ->
        AdminErrorDialog(message = message, onDismiss = onClearError)
    }
}
