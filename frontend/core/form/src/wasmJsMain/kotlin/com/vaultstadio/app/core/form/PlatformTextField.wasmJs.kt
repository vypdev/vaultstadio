package com.vaultstadio.app.core.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

@Composable
internal actual fun PlatformTextField(field: TextFieldSpec, modifier: Modifier, content: TextFieldContent) {
    if (content.appearance == TextFieldAppearance.PLAIN) {
        NativeTextEditor(field, modifier, content)
    } else {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ProvideTextStyle(MaterialTheme.typography.bodySmall) { content.label?.invoke() }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                content.leadingIcon?.let { Box { it() } }
                NativeTextEditor(field, Modifier.weight(1f), content)
                content.trailingIcon?.let { Box { it() } }
            }
            ProvideTextStyle(MaterialTheme.typography.bodySmall) { content.supportingText?.invoke() }
        }
    }
}

@Composable
private fun NativeTextEditor(field: TextFieldSpec, modifier: Modifier, content: TextFieldContent) {
    val handles = remember { mutableMapOf<HTMLElement, WebFieldHandle>() }
    val style = webTextStyle(field, content)
    val density = LocalDensity.current
    var width by remember { mutableIntStateOf(0) }
    var height by remember(field.singleLine, field.minLines) { mutableStateOf(56.dp) }
    val outlined = content.appearance == TextFieldAppearance.OUTLINED
    val sized = if (outlined) modifier.height(height) else modifier
    key(field.singleLine, field.isPassword) {
        HtmlElementView(
            factory = {
                (document.createElement(if (field.singleLine) "input" else "textarea") as HTMLElement).also { element ->
                    handles[element] = WebFieldHandle(WebTextEditor(element, field), WebTextMeasurement(element))
                }
            },
            modifier = sized.onSizeChanged { width = it.width },
            update = { element ->
                val handle = handles.getValue(element)
                handle.editor.update(field)
                style.apply(element, field, content)
                if (outlined) handle.measurement.schedule(field, style, width, density) { height = it }
            },
            onRelease = { element -> handles.remove(element)?.close() },
        )
    }
}

private class WebFieldHandle(val editor: WebTextEditor, val measurement: WebTextMeasurement) {
    fun close() {
        editor.close()
        measurement.close()
    }
}
