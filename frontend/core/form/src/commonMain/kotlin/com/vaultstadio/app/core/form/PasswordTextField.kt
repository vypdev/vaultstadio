package com.vaultstadio.app.core.form

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction

/** A controlled secure field with a platform-owned editor and application-owned callbacks. */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    identifier: String,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    passwordVisible: Boolean = false,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
) {
    val field = PasswordFieldSpec(
        value, onValueChange, identifier, label, enabled, readOnly,
        isError, passwordVisible, imeAction, onImeAction,
    )
    PlatformPasswordField(field, modifier, leadingIcon, trailingIcon, supportingText)
}

internal class PasswordFieldSpec(
    val value: String,
    val onValueChange: (String) -> Unit,
    val identifier: String,
    val label: String,
    val enabled: Boolean = true,
    val readOnly: Boolean = false,
    val isError: Boolean = false,
    val passwordVisible: Boolean = false,
    val imeAction: ImeAction = ImeAction.Default,
    val onImeAction: (() -> Unit)? = null,
) {
    init {
        require(identifier.isNotBlank())
        require(label.isNotBlank())
    }
}

@Composable
internal expect fun PlatformPasswordField(
    field: PasswordFieldSpec,
    modifier: Modifier,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    supportingText: @Composable (() -> Unit)?,
)
