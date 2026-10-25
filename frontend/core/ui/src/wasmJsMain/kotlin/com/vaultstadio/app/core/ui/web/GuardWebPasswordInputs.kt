@file:OptIn(ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.core.ui.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import kotlin.js.ExperimentalWasmJsInterop

/** Composition owns the observer lifetime and supplies the current localized name. */
@Composable
fun GuardWebPasswordInputs(viewportContainerId: String, passwordLabel: String) {
    DisposableEffect(viewportContainerId, passwordLabel) {
        val viewport = checkNotNull(document.getElementById(viewportContainerId) as? HTMLElement)
        val host = composeWebInputHost(viewport)
        val bridge = ProtectedWebInputBridge(host, passwordLabel)
        onDispose { bridge.close() }
    }
}
