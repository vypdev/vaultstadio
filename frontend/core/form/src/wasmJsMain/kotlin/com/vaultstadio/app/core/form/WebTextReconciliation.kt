package com.vaultstadio.app.core.form

import kotlinx.browser.window

/** Reconciles rejected edits after the caller has had a render turn; preserves active IME. */
internal class WebTextReconciliation(
    private val control: WebTextControl,
    private val currentField: () -> TextFieldSpec,
) {
    private var frame: Int? = null
    private var closed = false
    var isComposing = false
        private set

    fun compositionStarted() {
        isComposing = true
    }

    fun compositionEnded() {
        isComposing = false
        schedule()
    }

    fun schedule() {
        if (closed) return
        frame?.let { window.cancelAnimationFrame(it) }
        frame = window.requestAnimationFrame {
            if (!closed) {
                frame = window.requestAnimationFrame { afterRender() }
            }
        }
    }

    private fun afterRender() {
        frame = null
        if (!closed) sync(currentField())
    }

    fun sync(field: TextFieldSpec, selection: WebTextSelection = control.selection(), typeChanged: Boolean = false) {
        if (closed || preservesComposition(field)) return
        val changed = control.value != field.value
        if (changed) control.value = field.value
        if (changed || typeChanged) control.restoreSelection(selection, field.value.length)
    }

    private fun preservesComposition(field: TextFieldSpec): Boolean = isComposing && field.enabled && !field.readOnly

    fun close() {
        if (closed) return
        closed = true
        frame?.let { window.cancelAnimationFrame(it) }
        frame = null
    }
}
