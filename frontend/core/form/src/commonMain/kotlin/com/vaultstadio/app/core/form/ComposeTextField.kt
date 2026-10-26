package com.vaultstadio.app.core.form

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.vaultstadio.app.core.ui.inputSemantics

@Composable
internal fun ComposeTextField(field: TextFieldSpec, modifier: Modifier, content: TextFieldContent) {
    if (content.appearance == TextFieldAppearance.PLAIN) {
        PlainComposeTextField(field, modifier, content)
    } else {
        OutlinedComposeTextField(field, modifier, content)
    }
}

@Composable
private fun OutlinedComposeTextField(field: TextFieldSpec, modifier: Modifier, content: TextFieldContent) {
    OutlinedTextField(
        value = field.value,
        onValueChange = field.onValueChange,
        label = content.label,
        placeholder = content.placeholder?.let { text -> { Text(text) } },
        modifier = modifier.inputSemantics(field.identifier, field.label),
        enabled = field.enabled,
        readOnly = field.readOnly,
        isError = field.isError,
        singleLine = field.singleLine,
        minLines = field.minLines,
        maxLines = field.maxLines,
        textStyle = LocalTextStyle.current.merge(content.textStyle),
        shape = content.shape ?: OutlinedTextFieldDefaults.shape,
        visualTransformation = if (field.isPassword && !field.passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = field.keyboardOptions,
        keyboardActions = field.keyboardActions(),
        leadingIcon = content.leadingIcon,
        trailingIcon = content.trailingIcon,
        supportingText = content.supportingText,
    )
}

@Composable
private fun PlainComposeTextField(field: TextFieldSpec, modifier: Modifier, content: TextFieldContent) {
    BasicTextField(
        value = field.value,
        onValueChange = field.onValueChange,
        modifier = modifier.inputSemantics(field.identifier, field.label),
        enabled = field.enabled,
        readOnly = field.readOnly,
        singleLine = field.singleLine,
        minLines = field.minLines,
        maxLines = field.maxLines,
        textStyle = content.textStyle,
        cursorBrush = content.cursorBrush ?: SolidColor(Color.Black),
        keyboardOptions = field.keyboardOptions,
        keyboardActions = field.keyboardActions(),
    )
}

private fun TextFieldSpec.keyboardActions(): KeyboardActions =
    onImeAction?.let { action -> KeyboardActions(onAny = { action() }) } ?: KeyboardActions.Default
