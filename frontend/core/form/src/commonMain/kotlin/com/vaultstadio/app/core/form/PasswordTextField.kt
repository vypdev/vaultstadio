package com.vaultstadio.app.core.form

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

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
    val field = TextFieldSpec(
        value = value,
        onValueChange = onValueChange,
        identifier = identifier,
        label = label,
        enabled = enabled,
        readOnly = readOnly,
        isError = isError,
        passwordVisible = passwordVisible,
        isPassword = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            autoCorrectEnabled = false,
            imeAction = imeAction,
        ),
        onImeAction = onImeAction,
    )
    PlatformTextField(
        field,
        modifier,
        TextFieldContent(
            label = { Text(label) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            supportingText = supportingText,
        ),
    )
}
