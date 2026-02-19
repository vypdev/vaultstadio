/**
 * WASM implementation of route sync using the browser History API.
 */

package com.vaultstadio.app.platform

import kotlinx.browser.window

actual fun getInitialPath(): String =
    window.location.pathname.ifEmpty { "/" }

actual fun setPath(path: String) {
    val title = "VaultStadio"
    val url = path.ifEmpty { "/" }
    val current = window.location.pathname.ifEmpty { "/" }
    if (current == url) {
        window.history.replaceState(null, title, url)
    } else {
        window.history.pushState(null, title, url)
    }
}

private var popStateCallback: (() -> Unit)? = null

actual fun setOnPopState(callback: () -> Unit) {
    popStateCallback = callback
    window.onpopstate = {
        popStateCallback?.invoke()
    }
}
