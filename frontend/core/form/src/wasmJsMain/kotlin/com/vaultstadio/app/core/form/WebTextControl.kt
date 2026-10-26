package com.vaultstadio.app.core.form

import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement

/** Typed access to the two standard editable elements that this module creates. */
internal class WebTextControl(val element: HTMLElement) {
    init {
        require(element is HTMLInputElement || element is HTMLTextAreaElement)
    }

    var value: String
        get() = when (val node = element) {
            is HTMLInputElement -> node.value
            is HTMLTextAreaElement -> node.value
            else -> error("Unsupported native text control")
        }
        set(value) {
            when (val node = element) {
                is HTMLInputElement -> node.value = value
                is HTMLTextAreaElement -> node.value = value
            }
        }

    fun selection(): WebTextSelection = when (val node = element) {
        is HTMLInputElement -> WebTextSelection(node.selectionStart, node.selectionEnd, node.selectionDirection)
        is HTMLTextAreaElement -> WebTextSelection(node.selectionStart, node.selectionEnd, node.selectionDirection)
        else -> error("Unsupported native text control")
    }

    fun restoreSelection(selection: WebTextSelection, length: Int) {
        val start = selection.start?.coerceAtMost(length) ?: return
        val end = selection.end?.coerceAtMost(length) ?: return
        val direction = selection.direction ?: "none"
        when (val node = element) {
            is HTMLInputElement -> node.setSelectionRange(start, end, direction)
            is HTMLTextAreaElement -> node.setSelectionRange(start, end, direction)
        }
    }

    fun updateProtection(field: TextFieldSpec): Boolean {
        val input = element as? HTMLInputElement ?: return false
        val type = if (field.isPassword && !field.passwordVisible) "password" else "text"
        val changed = input.type != type
        if (changed) input.type = type
        return changed
    }
}

internal data class WebTextSelection(val start: Int?, val end: Int?, val direction: String?)
