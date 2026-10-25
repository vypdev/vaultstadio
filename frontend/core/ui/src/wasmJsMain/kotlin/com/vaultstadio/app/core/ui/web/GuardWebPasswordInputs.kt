@file:OptIn(ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.core.ui.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.browser.document
import kotlin.js.ExperimentalWasmJsInterop

/** Composition owns the observer lifetime and supplies the current localized name. */
@Composable
fun GuardWebPasswordInputs(passwordLabel: String) {
    DisposableEffect(passwordLabel) {
        val host = checkNotNull(document.body)
        val bridge = ProtectedWebInputBridge(host, passwordLabel)
        onDispose { bridge.close() }
    }
}
