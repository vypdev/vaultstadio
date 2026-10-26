package com.vaultstadio.app.core.form

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTextAreaElement

/** Measures only an owned textarea; never changes its content, selection or events. */
internal class WebTextMeasurement(private val element: HTMLElement) {
    private var frame: Int? = null
    private var closed = false

    fun schedule(
        field: TextFieldSpec,
        style: WebTextStyle,
        layoutWidth: Int,
        density: Density,
        onHeight: (Dp) -> Unit,
    ) {
        if (closed || element !is HTMLTextAreaElement) return
        frame?.let { window.cancelAnimationFrame(it) }
        frame = window.requestAnimationFrame {
            frame = null
            if (!closed && element.isConnected) {
                measure(field, style, layoutWidth, density)?.let(onHeight)
            }
        }
    }

    private fun measure(field: TextFieldSpec, style: WebTextStyle, layoutWidth: Int, density: Density): Dp? {
        val cssWidth = element.getBoundingClientRect().width
        if (cssWidth <= 0 || layoutWidth <= 0) return null
        val dpPerCssPixel = layoutWidth / (cssWidth * density.density)
        val previousHeight = element.style.height
        val needed = try {
            element.style.height = "auto"
            element.scrollHeight + BORDER_PIXELS
        } finally {
            element.style.height = previousHeight
        }
        val minimum = maxOf(56.0 / dpPerCssPixel, style.lineHeight.toDouble() * field.minLines + PADDING_PIXELS)
        val maximum = maxOf(minimum, style.lineHeight.toDouble() * field.maxLines + PADDING_PIXELS)
        return (needed.toDouble().coerceIn(minimum, maximum) * dpPerCssPixel).toFloat().dp
    }

    fun close() {
        if (closed) return
        closed = true
        frame?.let { window.cancelAnimationFrame(it) }
        frame = null
    }

    private companion object {
        const val BORDER_PIXELS = 2
        const val PADDING_PIXELS = 26
    }
}
