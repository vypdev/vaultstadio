package com.vaultstadio.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.vaultstadio.app.domain.auth.model.StorageQuota
import com.vaultstadio.app.domain.auth.model.User
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User?,
    quota: StorageQuota?,
    isLoading: Boolean,
    isSaving: Boolean,
    error: String?,
    successMessage: String?,
    onRefresh: () -> Unit,
    onUpdateProfile: (username: String) -> Unit,
    onChangePassword: (currentPassword: String, newPassword: String) -> Unit,
    onClearError: () -> Unit,
    onClearSuccessMessage: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToChangePassword: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onExportData: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            onClearError()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 12.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                    IconButton(onClick = { showEditProfileDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (isLoading && user == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    ProfileHeader(
                        name = user?.username ?: "User",
                        email = user?.email ?: "",
                        memberSince = user?.createdAt?.let { instant ->
                            val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                            "${getMonthName(localDate.monthNumber)} ${localDate.year}"
                        } ?: "",
                    )
                }

                item {
                    quota?.let { q ->
                        StorageQuotaCard(
                            usedBytes = q.usedBytes,
                            totalBytes = q.quotaBytes ?: 0L,
                            usedPercentage = q.usagePercentage.toFloat(),
                        )
                    } ?: StorageQuotaCard(
                        usedBytes = 0L,
                        totalBytes = 0L,
                        usedPercentage = 0f,
                    )
                }

                item {
                    QuickStatsCard(quota = quota)
                }

                item {
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                item {
                    AccountActionsCard(
                        onChangePassword = onNavigateToChangePassword,
                        onSecuritySettings = onNavigateToSecurity,
                        onConnectedDevices = onNavigateToSecurity,
                        onLoginHistory = onNavigateToSecurity,
                        onExportData = onExportData,
                    )
                }
            }
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentUsername = user?.username ?: "",
            isSaving = isSaving,
            onSave = { username ->
                if (username.isNotBlank()) {
                    onUpdateProfile(username)
                    showEditProfileDialog = false
                }
            },
            onDismiss = { showEditProfileDialog = false },
        )
    }
}
