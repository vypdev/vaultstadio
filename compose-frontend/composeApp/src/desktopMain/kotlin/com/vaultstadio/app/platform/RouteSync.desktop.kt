/**
 * Desktop route sync: no URL bar; returns default path for consistency.
 */

package com.vaultstadio.app.platform

actual fun getInitialPath(): String = "/"

actual fun setPath(path: String) {
    // No URL bar on desktop.
}

actual fun setOnPopState(callback: () -> Unit) {
    // No browser history on desktop.
}
