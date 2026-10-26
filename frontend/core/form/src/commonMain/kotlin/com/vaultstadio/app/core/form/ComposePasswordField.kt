package com.vaultstadio.app.core.form

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.vaultstadio.app.core.ui.inputSemantics

@Composable
internal fun ComposePasswordField(
    field: PasswordFieldSpec,
    modifier: Modifier,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    supportingText: @Composable (() -> Unit)?,
) {
    OutlinedTextField(
        value = field.value,
        onValueChange = field.onValueChange,
        label = { Text(field.label) },
        modifier = modifier.inputSemantics(field.identifier, field.label),
        enabled = field.enabled,
        readOnly = field.readOnly,
        isError = field.isError,
        singleLine = true,
        visualTransformation = if (field.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            autoCorrectEnabled = false,
            imeAction = field.imeAction,
        ),
        keyboardActions = field.onImeAction?.let { action -> KeyboardActions(onAny = { action() }) }
            ?: KeyboardActions.Default,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
    )
}
