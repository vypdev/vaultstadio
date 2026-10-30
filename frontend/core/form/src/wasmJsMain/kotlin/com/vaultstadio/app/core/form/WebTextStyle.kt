package com.vaultstadio.app.core.form

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import org.w3c.dom.HTMLElement
import kotlin.math.roundToInt

internal data class WebTextStyle(
    val fontSize: Float,
    val lineHeight: Float,
    private val color: Color,
    private val background: Color,
    private val border: Color,
    private val caret: Color,
    private val family: String,
    private val radius: Float,
    private val plain: Boolean,
) {
    fun apply(element: HTMLElement, field: TextFieldSpec, content: TextFieldContent) {
        element.style.apply {
            width = "100%"
            height = "100%"
            boxSizing = "border-box"
            padding = if (plain) "0" else "12px"
            border = if (plain) "0" else "1px solid"
            borderRadius = "${radius}px"
            backgroundColor = if (plain) "transparent" else this@WebTextStyle.background.css()
            color = this@WebTextStyle.color.css()
            borderColor = this@WebTextStyle.border.css()
            setProperty("caret-color", caret.css())
            fontSize = "${this@WebTextStyle.fontSize}px"
            lineHeight = "${this@WebTextStyle.lineHeight}px"
            fontFamily = family
            resize = "none"
        }
        element.setAttribute("placeholder", content.placeholder.orEmpty())
        if (!field.singleLine) element.setAttribute("rows", field.minLines.toString())
    }
}

@Composable
internal fun webTextStyle(field: TextFieldSpec, content: TextFieldContent): WebTextStyle {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val style = MaterialTheme.typography.bodyLarge.merge(content.textStyle)
    val fontSize = if (style.fontSize == TextUnit.Unspecified) 16f else style.fontSize.value
    val lineHeight = if (style.lineHeight == TextUnit.Unspecified) fontSize * 1.5f else style.lineHeight.value
    val color = if (style.color == Color.Unspecified) colors.onSurface else style.color
    val radius = (content.shape as? CornerBasedShape)?.topStart?.toPx(Size.Zero, density)
        ?.div(density.density) ?: 4f
    return WebTextStyle(
        fontSize * density.fontScale,
        lineHeight * density.fontScale,
        color,
        colors.surface,
        if (field.isError) colors.error else colors.outline,
        (content.cursorBrush as? SolidColor)?.value ?: colors.primary,
        if (style.fontFamily == FontFamily.Monospace) "monospace" else "sans-serif",
        radius,
        content.appearance == TextFieldAppearance.PLAIN,
    )
}

private fun Color.css(): String {
    val channels = listOf(red, green, blue).map { (it * 255).roundToInt() }
    return "rgba(${channels.joinToString(",")},$alpha)"
}
