/**
 * VaultStadio App Configuration
 *
 * Provides composition locals for app-wide configuration.
 */

package com.vaultstadio.app.config

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal for the API base URL.
 * This allows UI components to access the API URL for image loading, downloads, etc.
 */
val LocalApiBaseUrl = compositionLocalOf { "http://localhost:8080" }
