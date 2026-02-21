package com.vaultstadio.app.feature.federation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.core.resources.StringResources
import com.vaultstadio.app.domain.federation.model.FederatedInstance

@Composable
fun RequestFederationDialog(
    strings: StringResources,
    onDismiss: () -> Unit,
    onConfirm: (domain: String, message: String?) -> Unit,
) {
    var domain by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.federationRequest) },
        text = {
            Column {
                Text(
                    strings.federationEnterDomain,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text(strings.federationInstanceDomain) },
                    placeholder = { Text(strings.federationInstanceDomainPlaceholder) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text(strings.federationMessageOptional) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (domain.isNotBlank()) {
                        onConfirm(domain, message.takeIf { it.isNotBlank() })
                    }
                },
            ) {
                Text(strings.commonRequest)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel)
            }
        },
    )
}

@Composable
fun BlockInstanceDialog(
    instance: FederatedInstance,
    strings: StringResources,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.federationBlock) },
        text = { Text(strings.federationBlockConfirm) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(strings.commonBlock)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel)
            }
        },
    )
}

@Composable
fun InstanceDetailsDialog(
    instance: FederatedInstance,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Instance Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Name", instance.name)
                DetailRow("Domain", instance.domain)
                DetailRow("Status", instance.status.name)
                DetailRow("Version", instance.version)
                instance.lastSeenAt?.let { DetailRow("Last Seen", formatFederationRelativeTime(it)) }
                DetailRow("Registered", formatFederationRelativeTime(instance.registeredAt))
                instance.description?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (instance.capabilities.isNotEmpty()) {
                    Text(
                        "Capabilities: ${instance.capabilities.joinToString(", ") { it.name }}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
fun LinkIdentityDialog(
    strings: StringResources,
    onDismiss: () -> Unit,
    onConfirm: (remoteUserId: String, remoteInstance: String, displayName: String) -> Unit,
) {
    var remoteUserId by remember { mutableStateOf("") }
    var remoteInstance by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link Federated Identity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = remoteUserId,
                    onValueChange = { remoteUserId = it },
                    label = { Text("Remote User ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = remoteInstance,
                    onValueChange = { remoteInstance = it },
                    label = { Text("Remote Instance (e.g., other.vaultstadio.com)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (remoteUserId.isNotBlank() && remoteInstance.isNotBlank() &&
                        displayName.isNotBlank()
                    ) {
                        onConfirm(remoteUserId, remoteInstance, displayName)
                    }
                },
                enabled = remoteUserId.isNotBlank() && remoteInstance.isNotBlank() &&
                    displayName.isNotBlank(),
            ) {
                Text("Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel)
            }
        },
    )
}

@Composable
fun ErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(errorMessage) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}
