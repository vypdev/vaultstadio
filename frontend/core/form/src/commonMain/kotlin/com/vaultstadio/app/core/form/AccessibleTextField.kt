package com.vaultstadio.app.core.form

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle

/** Keeps feature policy/callbacks shared while each platform owns its editable control. */
@Composable
fun AccessibleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    identifier: String,
    accessibleName: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onImeAction: (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    placeholder: String? = null,
    textStyle: TextStyle = LocalTextStyle.current,
    shape: Shape? = null,
) {
    PlatformTextField(
        TextFieldSpec(
            value = value,
            onValueChange = onValueChange,
            identifier = identifier,
            label = accessibleName,
            enabled = enabled,
            readOnly = readOnly,
            isError = isError,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            onImeAction = onImeAction,
        ),
        modifier,
        TextFieldContent(
            label = label,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            supportingText = supportingText,
            placeholder = placeholder,
            textStyle = textStyle,
            shape = shape,
        ),
    )
}
