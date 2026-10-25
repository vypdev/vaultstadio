@file:OptIn(ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.core.ui.web

import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.MutationObserver
import org.w3c.dom.MutationObserverInit
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsArray
import kotlin.js.JsString

/** Protects only Compose's transient native editor; never reads its credential value. */
internal class ProtectedWebInputBridge(
    private val host: HTMLElement,
    private val passwordLabel: String,
) {
    private var closed = false
    private var observedRoot: Node? = null
    private val observer = MutationObserver { _, _ -> refresh() }
    private val onFocus: (Event) -> Unit = { refresh() }

    init {
        require(passwordLabel.isNotBlank())
        host.addEventListener("focusin", onFocus, true)
        refresh()
    }

    internal fun refresh() {
        if (closed) return
        val root = host.shadowRoot ?: host
        if (observedRoot !== root) {
            observer.disconnect()
            observer.observe(
                root,
                MutationObserverInit(
                    childList = true,
                    subtree = true,
                    attributes = true,
                    attributeFilter = inputModeAttributeFilter(),
                ),
            )
            observedRoot = root
        }
        val inputs = host.shadowRoot?.querySelectorAll("input") ?: host.querySelectorAll("input")
        for (index in 0 until inputs.length) {
            val input = inputs.item(index) as HTMLInputElement
            if (isFrameworkPasswordEditor(input)) protect(input)
        }
    }

    private fun isFrameworkPasswordEditor(input: HTMLInputElement): Boolean =
        input.getAttribute("style").orEmpty().contains("--compose-internal-web-backing-input") &&
            input.getAttribute("inputmode") == "password"

    private fun protect(input: HTMLInputElement) {
        if (input.type != "password") {
            val start = input.selectionStart
            val end = input.selectionEnd
            val direction = input.selectionDirection
            input.type = "password"
            if (start != null && end != null) input.setSelectionRange(start, end, direction ?: "none")
        }
        val label = input.getAttribute("aria-label")
        val ownedLabel = input.getAttribute(OWNED_LABEL)
        if (label.isNullOrBlank() || label == ownedLabel) {
            if (label != passwordLabel) input.setAttribute("aria-label", passwordLabel)
            if (ownedLabel != passwordLabel) input.setAttribute(OWNED_LABEL, passwordLabel)
        } else if (ownedLabel != null) {
            input.removeAttribute(OWNED_LABEL)
        }
    }

    internal fun close() {
        if (closed) return
        closed = true
        observer.disconnect()
        host.removeEventListener("focusin", onFocus, true)
        observedRoot = null
        // A connected password editor stays protected until the framework removes it.
    }

    private companion object {
        const val OWNED_LABEL = "data-vaultstadio-protected-input-label"
    }
}

private fun inputModeAttributeFilter(): JsArray<JsString> = js("['inputmode']")
