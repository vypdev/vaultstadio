package com.vaultstadio.app.feature.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.auth.model.ActiveSession
import com.vaultstadio.app.domain.auth.model.LoginEvent
import com.vaultstadio.app.domain.auth.model.SecuritySettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    isLoading: Boolean,
    securitySettings: SecuritySettings?,
    sessions: List<ActiveSession>,
    loginHistory: List<LoginEvent>,
    showRevokeSessionDialog: ActiveSession?,
    errorMessage: String?,
    onToggleTwoFactor: () -> Unit,
    onShowRevokeDialog: (ActiveSession) -> Unit,
    onDismissRevokeDialog: () -> Unit,
    onRevokeSession: (ActiveSession) -> Unit,
    onDismissError: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissError()
        }
    }

    showRevokeSessionDialog?.let { session ->
        RevokeSessionDialog(
            session = session,
            onConfirm = { onRevokeSession(session) },
            onDismiss = onDismissRevokeDialog,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        },
        modifier = modifier,
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                item { SectionHeader("Two-Factor Authentication") }
                item {
                    TwoFactorCard(
                        enabled = securitySettings?.twoFactorEnabled ?: false,
                        onToggle = onToggleTwoFactor,
                    )
                }

                item { SectionHeader("Connected Devices") }
                if (sessions.isEmpty()) {
                    item { EmptyStateCard("No other active sessions") }
                } else {
                    items(sessions) { session ->
                        SessionCard(
                            session = session,
                            onRevoke = { onShowRevokeDialog(session) },
                        )
                    }
                }

                item { SectionHeader("Recent Login Activity") }
                if (loginHistory.isEmpty()) {
                    item { EmptyStateCard("No login history available") }
                } else {
                    items(loginHistory) { event ->
                        LoginEventCard(event = event)
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
