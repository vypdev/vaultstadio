package com.vaultstadio.app.core.form

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import org.w3c.dom.HTMLElement

internal fun HTMLElement.applyTextMetadata(field: TextFieldSpec) {
    id = field.identifier
    booleanAttribute("disabled", !field.enabled)
    booleanAttribute("readonly", field.readOnly)
    setAttribute("aria-label", field.label)
    setAttribute("aria-invalid", field.isError.toString())
    setAttribute("autocomplete", field.autocomplete())
    val correction = !field.isPassword && field.keyboardOptions.autoCorrectEnabled != false
    setAttribute("autocorrect", if (correction) "on" else "off")
    setAttribute("spellcheck", correction.toString())
    setAttribute("autocapitalize", field.keyboardOptions.capitalization.htmlValue())
    setAttribute("inputmode", field.keyboardOptions.keyboardType.htmlValue())
    setAttribute("enterkeyhint", field.imeAction.htmlValue())
}

private fun HTMLElement.booleanAttribute(name: String, present: Boolean) {
    if (present) setAttribute(name, "") else removeAttribute(name)
}

private fun TextFieldSpec.autocomplete(): String = when {
    !isPassword -> "off"
    identifier.contains("current") || identifier.contains("login") -> "current-password"
    else -> "new-password"
}

private fun KeyboardCapitalization.htmlValue(): String = when (this) {
    KeyboardCapitalization.Words -> "words"
    KeyboardCapitalization.Sentences -> "sentences"
    KeyboardCapitalization.Characters -> "characters"
    else -> "off"
}

private fun KeyboardType.htmlValue(): String = when (this) {
    KeyboardType.Email -> "email"
    KeyboardType.Uri -> "url"
    KeyboardType.Phone -> "tel"
    KeyboardType.Number, KeyboardType.NumberPassword -> "numeric"
    KeyboardType.Decimal -> "decimal"
    else -> "text"
}

private fun ImeAction.htmlValue(): String = when (this) {
    ImeAction.Next -> "next"
    ImeAction.Previous -> "previous"
    ImeAction.Done -> "done"
    ImeAction.Search -> "search"
    ImeAction.Go -> "go"
    ImeAction.Send -> "send"
    else -> "enter"
}
