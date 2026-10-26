package com.vaultstadio.app.core.form

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/** Input state intentionally has no generated value-bearing toString. */
internal class TextFieldSpec(
    val value: String,
    val onValueChange: (String) -> Unit,
    val identifier: String,
    val label: String,
    val enabled: Boolean = true,
    val readOnly: Boolean = false,
    val isError: Boolean = false,
    val passwordVisible: Boolean = false,
    val onImeAction: (() -> Unit)? = null,
    val isPassword: Boolean = false,
    val singleLine: Boolean = true,
    val minLines: Int = 1,
    val maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    val keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val imeAction get() = keyboardOptions.imeAction

    init {
        require(identifier.isNotBlank())
        require(label.isNotBlank())
        require(minLines > 0 && maxLines >= minLines)
        require(!isPassword || singleLine)
        require(isPassword || keyboardOptions.keyboardType !in secretKeyboardTypes)
    }
}

private val secretKeyboardTypes = setOf(KeyboardType.Password, KeyboardType.NumberPassword)

@Composable
internal expect fun PlatformTextField(field: TextFieldSpec, modifier: Modifier, content: TextFieldContent)
