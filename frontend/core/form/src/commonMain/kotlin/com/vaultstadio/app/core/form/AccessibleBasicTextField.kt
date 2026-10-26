package com.vaultstadio.app.core.form

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle

@Composable
fun AccessibleBasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    identifier: String,
    accessibleName: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onImeAction: (() -> Unit)? = null,
    textStyle: TextStyle = TextStyle.Default,
    cursorBrush: Brush = SolidColor(Color.Black),
) {
    PlatformTextField(
        TextFieldSpec(
            value = value,
            onValueChange = onValueChange,
            identifier = identifier,
            label = accessibleName,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            onImeAction = onImeAction,
        ),
        modifier,
        TextFieldContent(
            appearance = TextFieldAppearance.PLAIN,
            textStyle = textStyle,
            cursorBrush = cursorBrush,
        ),
    )
}
