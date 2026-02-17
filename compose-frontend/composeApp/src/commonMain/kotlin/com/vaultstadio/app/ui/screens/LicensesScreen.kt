/**
 * VaultStadio Licenses Screen
 *
 * Screen displaying open source licenses for third-party libraries.
 */

package com.vaultstadio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.i18n.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsLicenses) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Text(
                    text = "VaultStadio uses the following open source libraries:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(openSourceLicenses) { license ->
                LicenseCard(license = license)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LicenseCard(license: OpenSourceLicense) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = license.name,
                style = MaterialTheme.typography.titleSmall,
            )

            Text(
                text = license.version,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = license.licenseType,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            if (license.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = license.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class OpenSourceLicense(
    val name: String,
    val version: String,
    val licenseType: String,
    val description: String = "",
)

private val openSourceLicenses = listOf(
    OpenSourceLicense(
        name = "Kotlin",
        version = "2.0+",
        licenseType = "Apache License 2.0",
        description = "The Kotlin programming language",
    ),
    OpenSourceLicense(
        name = "Compose Multiplatform",
        version = "1.7+",
        licenseType = "Apache License 2.0",
        description = "Declarative UI framework by JetBrains",
    ),
    OpenSourceLicense(
        name = "Decompose",
        version = "3.0+",
        licenseType = "Apache License 2.0",
        description = "Lifecycle-aware navigation library",
    ),
    OpenSourceLicense(
        name = "Koin",
        version = "4.0+",
        licenseType = "Apache License 2.0",
        description = "Dependency injection framework for Kotlin",
    ),
    OpenSourceLicense(
        name = "Ktor",
        version = "3.0+",
        licenseType = "Apache License 2.0",
        description = "Asynchronous HTTP client for Kotlin",
    ),
    OpenSourceLicense(
        name = "Coil",
        version = "3.0+",
        licenseType = "Apache License 2.0",
        description = "Image loading library for Compose",
    ),
    OpenSourceLicense(
        name = "kotlinx.serialization",
        version = "1.7+",
        licenseType = "Apache License 2.0",
        description = "Kotlin multiplatform serialization library",
    ),
    OpenSourceLicense(
        name = "kotlinx.coroutines",
        version = "1.9+",
        licenseType = "Apache License 2.0",
        description = "Coroutines support for Kotlin",
    ),
    OpenSourceLicense(
        name = "kotlinx-datetime",
        version = "0.6+",
        licenseType = "Apache License 2.0",
        description = "Date and time library for Kotlin",
    ),
    OpenSourceLicense(
        name = "Material Design Icons",
        version = "Latest",
        licenseType = "Apache License 2.0",
        description = "Material Design icon set by Google",
    ),
    OpenSourceLicense(
        name = "Arrow",
        version = "1.2+",
        licenseType = "Apache License 2.0",
        description = "Functional companion to Kotlin's Standard Library",
    ),
)
