package com.vaultstadio.app.core.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import kotlin.math.roundToInt

@Composable
internal actual fun PlatformPasswordField(
    field: PasswordFieldSpec,
    modifier: Modifier,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    supportingText: @Composable (() -> Unit)?,
) {
    val editors = remember { mutableMapOf<HTMLInputElement, WebPasswordEditor>() }
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(field.label, style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (leadingIcon != null) Box { leadingIcon() }
            HtmlElementView(
                factory = {
                    (document.createElement("input") as HTMLInputElement).also { input ->
                        editors[input] = WebPasswordEditor(input, field)
                        input.style.apply {
                            width = "100%"
                            height = "100%"
                            boxSizing = "border-box"
                            border = "1px solid"
                            borderRadius = "4px"
                            padding = "12px"
                            fontSize = "16px"
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(56.dp),
                update = { input ->
                    editors.getValue(input).update(field)
                    input.style.backgroundColor = colors.surface.css()
                    input.style.color = colors.onSurface.css()
                    input.style.borderColor = (if (field.isError) colors.error else colors.outline).css()
                },
                onRelease = { input -> editors.remove(input)?.close() },
            )
            if (trailingIcon != null) Box { trailingIcon() }
        }
        supportingText?.invoke()
    }
}

private fun Color.css(): String {
    val channels = listOf(red, green, blue).map { (it * 255).roundToInt() }
    return "rgba(${channels.joinToString(",")},$alpha)"
}
