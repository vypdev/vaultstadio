package com.vaultstadio.app.core.form

import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

/** Owns one standard text control; no access to the framework's backing editor. */
internal class WebTextEditor(
    private val input: HTMLElement,
    initial: TextFieldSpec,
) {
    private val control = WebTextControl(input)
    private var field = initial
    private var closed = false
    private val reconciliation = WebTextReconciliation(control) { field }
    private val onInput: (Event) -> Unit = {
        if (!closed) {
            if (field.enabled && !field.readOnly) {
                field.onValueChange(control.value)
                reconciliation.schedule()
            } else {
                control.value = field.value
            }
        }
    }
    private val onKey: (Event) -> Unit = { event ->
        val key = event as KeyboardEvent
        if (canInvokeImeAction && key.isPlainEnter()) {
            field.onImeAction?.let { action ->
                key.preventDefault()
                action()
            }
        }
    }

    private val canEdit get() = !closed && field.enabled && !field.readOnly
    private val canInvokeImeAction get() = canEdit && field.singleLine && !reconciliation.isComposing
    private val onCompositionStart: (Event) -> Unit = { reconciliation.compositionStarted() }
    private val onCompositionEnd: (Event) -> Unit = { reconciliation.compositionEnded() }

    init {
        update(initial)
        input.addEventListener("input", onInput)
        input.addEventListener("keydown", onKey)
        input.addEventListener("compositionstart", onCompositionStart)
        input.addEventListener("compositionend", onCompositionEnd)
    }

    fun update(next: TextFieldSpec) {
        if (closed) return
        field = next
        val selection = control.selection()
        val typeChanged = control.updateProtection(next)
        reconciliation.sync(next, selection, typeChanged)
        input.applyTextMetadata(next)
    }

    fun close() {
        if (closed) return
        closed = true
        input.removeEventListener("input", onInput)
        input.removeEventListener("keydown", onKey)
        input.removeEventListener("compositionstart", onCompositionStart)
        input.removeEventListener("compositionend", onCompositionEnd)
        reconciliation.close()
    }
}

private fun KeyboardEvent.isPlainEnter(): Boolean = key == "Enter" && !isComposing && !hasModifier()

private fun KeyboardEvent.hasModifier(): Boolean = altKey || ctrlKey || metaKey || shiftKey
