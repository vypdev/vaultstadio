package com.vaultstadio.app.feature.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import com.vaultstadio.app.core.resources.InputLabel
import com.vaultstadio.app.core.resources.LocalStrings
import com.vaultstadio.app.core.resources.inputLabel

@Composable
internal fun PasswordVisibilityToggle(visible: Boolean, onToggle: () -> Unit) {
    val strings = LocalStrings.current
    IconButton(onClick = onToggle) {
        Icon(
            if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = strings.inputLabel(
                if (visible) InputLabel.HIDE_PASSWORD else InputLabel.SHOW_PASSWORD,
            ),
        )
    }
}
