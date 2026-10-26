package com.vaultstadio.app.core.form

import androidx.compose.ui.text.input.ImeAction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

/** Owns one standard password input; no access to the framework's backing editor. */
internal class WebPasswordEditor(
    private val input: HTMLInputElement,
    initial: PasswordFieldSpec,
) {
    private var field = initial
    private var closed = false
    private val onInput: (Event) -> Unit = {
        if (!closed) {
            if (field.enabled && !field.readOnly) {
                field.onValueChange(input.value)
            } else {
                input.value = field.value
            }
        }
    }
    private val onKey: (Event) -> Unit = { event ->
        val key = event as KeyboardEvent
        if (canEdit && key.isPlainEnter()) {
            field.onImeAction?.let { action ->
                key.preventDefault()
                action()
            }
        }
    }

    private val canEdit get() = !closed && field.enabled && !field.readOnly

    init {
        update(initial)
        input.addEventListener("input", onInput)
        input.addEventListener("keydown", onKey)
    }

    fun update(next: PasswordFieldSpec) {
        if (closed) return
        field = next
        val start = input.selectionStart
        val end = input.selectionEnd
        val direction = input.selectionDirection
        val type = if (next.passwordVisible) "text" else "password"
        val selectionNeedsRestoring = input.type != type || input.value != next.value
        if (input.type != type) input.type = type
        if (input.value != next.value) input.value = next.value
        if (selectionNeedsRestoring && start != null && end != null) {
            input.setSelectionRange(
                start.coerceAtMost(next.value.length),
                end.coerceAtMost(next.value.length),
                direction ?: "none",
            )
        }
        input.id = next.identifier
        input.disabled = !next.enabled
        input.readOnly = next.readOnly
        input.setAttribute("aria-label", next.label)
        input.setAttribute("aria-invalid", next.isError.toString())
        input.setAttribute(
            "autocomplete",
            if (next.identifier.contains("current") ||
                next.identifier.contains("login")
            ) {
                "current-password"
            } else {
                "new-password"
            },
        )
        input.setAttribute("autocorrect", "off")
        input.setAttribute("autocapitalize", "off")
        input.setAttribute("spellcheck", "false")
        input.setAttribute("enterkeyhint", if (next.imeAction == ImeAction.Next) "next" else "done")
    }

    fun close() {
        if (closed) return
        closed = true
        input.removeEventListener("input", onInput)
        input.removeEventListener("keydown", onKey)
    }
}

private fun KeyboardEvent.isPlainEnter(): Boolean = key == "Enter" && !isComposing && !hasModifier()

private fun KeyboardEvent.hasModifier(): Boolean = altKey || ctrlKey || metaKey || shiftKey
