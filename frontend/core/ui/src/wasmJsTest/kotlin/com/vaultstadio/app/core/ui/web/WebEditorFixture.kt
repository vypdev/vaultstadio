@file:OptIn(ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.core.ui.web

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import kotlin.js.ExperimentalWasmJsInterop

internal class WebEditorFixture {
    val host = document.createElement("section") as HTMLElement
    private val bridges = mutableListOf<ProtectedWebInputBridge>()

    init {
        checkNotNull(document.body).appendChild(host)
    }

    fun input(mode: String = "password", frameworkOwned: Boolean = true): HTMLInputElement {
        val input = document.createElement("input") as HTMLInputElement
        input.setAttribute("inputmode", mode)
        if (frameworkOwned) input.style.setProperty("--compose-internal-web-backing-input-top", "0")
        input.value = "fixture-value"
        host.appendChild(input)
        return input
    }

    fun bridge(label: String = "Password", editorHost: HTMLElement = host) =
        ProtectedWebInputBridge(editorHost, label).also { bridges.add(it) }

    fun close() {
        bridges.forEach { it.close() }
        host.parentNode?.removeChild(host)
    }
}
