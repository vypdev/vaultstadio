@file:OptIn(ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.core.ui.web

import org.w3c.dom.HTMLElement
import kotlin.js.ExperimentalWasmJsInterop

/** Resolve only the framework host inside the app-owned viewport, never other shadow roots. */
internal fun composeWebInputHost(viewport: HTMLElement): HTMLElement {
    val candidates = mutableListOf<HTMLElement>()
    fun consider(element: HTMLElement) {
        val root = element.shadowRoot ?: return
        if (root.querySelector("canvas[role=generic]") != null && root.querySelector("#cmp_a11y_root") != null) {
            candidates.add(element)
        }
    }
    consider(viewport)
    val descendants = viewport.querySelectorAll("*")
    for (index in 0 until descendants.length) {
        (descendants.item(index) as? HTMLElement)?.let(::consider)
    }
    check(candidates.size == 1) { "Expected exactly one Compose shadow host inside the owned viewport" }
    return candidates.single()
}
