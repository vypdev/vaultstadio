/**
 * iOS route sync: no URL bar; returns default path for consistency.
 */

package com.vaultstadio.app.platform

actual fun getInitialPath(): String = "/"

actual fun setPath(path: String) {
    // No URL bar on iOS; path could be used for universal links in the future.
}

actual fun setOnPopState(callback: () -> Unit) {
    // No browser history on iOS.
}
