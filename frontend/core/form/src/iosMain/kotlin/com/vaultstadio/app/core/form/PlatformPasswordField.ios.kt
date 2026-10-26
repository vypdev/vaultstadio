package com.vaultstadio.app.core.form

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun PlatformPasswordField(
    field: PasswordFieldSpec,
    modifier: Modifier,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    supportingText: @Composable (() -> Unit)?,
) {
    ComposePasswordField(field, modifier, leadingIcon, trailingIcon, supportingText)
}
