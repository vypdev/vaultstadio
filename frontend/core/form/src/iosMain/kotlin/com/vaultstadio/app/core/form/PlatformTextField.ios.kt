package com.vaultstadio.app.core.form

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun PlatformTextField(field: TextFieldSpec, modifier: Modifier, content: TextFieldContent) {
    ComposeTextField(field, modifier, content)
}
