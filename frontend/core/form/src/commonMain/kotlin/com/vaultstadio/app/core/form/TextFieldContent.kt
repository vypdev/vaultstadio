package com.vaultstadio.app.core.form

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle

internal enum class TextFieldAppearance { OUTLINED, PLAIN }

internal class TextFieldContent(
    val appearance: TextFieldAppearance = TextFieldAppearance.OUTLINED,
    val label: @Composable (() -> Unit)? = null,
    val leadingIcon: @Composable (() -> Unit)? = null,
    val trailingIcon: @Composable (() -> Unit)? = null,
    val supportingText: @Composable (() -> Unit)? = null,
    val placeholder: String? = null,
    val textStyle: TextStyle = TextStyle.Default,
    val cursorBrush: Brush? = null,
    val shape: Shape? = null,
)
