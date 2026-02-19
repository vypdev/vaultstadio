/**
 * VaultStadio iOS Main View Controller
 *
 * Entry point for the iOS application, providing the Compose UI.
 */

package com.vaultstadio.app

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * Creates the main UIViewController for the iOS app.
 *
 * This is called from Swift/SwiftUI to embed the Compose UI.
 *
 * @return UIViewController hosting the Compose Multiplatform UI
 */
@Suppress("FunctionName")
fun MainViewController(): UIViewController {
    return ComposeUIViewController {
        VaultStadioApp()
    }
}
