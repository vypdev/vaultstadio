package com.vaultstadio.app.core.form

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlinx.browser.document
import kotlinx.coroutines.test.runTest
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTextAreaElement
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebTextMeasurementTest {
    private lateinit var container: HTMLElement
    private lateinit var textarea: HTMLTextAreaElement
    private lateinit var measurement: WebTextMeasurement
    private val style =
        WebTextStyle(16f, 24f, Color.Black, Color.White, Color.Black, Color.Black, "monospace", 4f, false)

    @BeforeTest fun prepare() {
        container = document.createElement("div") as HTMLElement
        container.style.width = "400px"
        document.body!!.appendChild(container)
        textarea = document.createElement("textarea") as HTMLTextAreaElement
        container.appendChild(textarea)
        style.apply(textarea, spec(""), TextFieldContent())
        measurement = WebTextMeasurement(textarea)
    }

    @AfterTest fun cleanup() {
        measurement.close()
        container.remove()
    }

    @Test fun multilineGrowthRespectsTheConfiguredMaximum() = runTest {
        val field = spec((1..10).joinToString("\n") { "line $it" }, maxLines = 3)
        textarea.value = field.value
        var height: Dp? = null
        measurement.schedule(field, style, 400, Density(1f)) { height = it }
        afterTwoFrames()
        assertTrue(height != null && height!!.value > 56f && height!!.value <= 100f)
    }

    @Test fun narrowingTheControlMeasuresWrappedTextAgain() = runTest {
        val field = spec("word ".repeat(20), maxLines = 5)
        textarea.value = field.value
        var wide: Dp? = null
        measurement.schedule(field, style, 400, Density(1f)) { wide = it }
        afterTwoFrames()
        container.style.width = "140px"
        var narrow: Dp? = null
        measurement.schedule(field, style, 140, Density(1f)) { narrow = it }
        afterTwoFrames()
        assertTrue(wide != null && narrow != null && narrow!! > wide!!)
    }

    @Test fun measuringKeepsContentAndSelection() = runTest {
        val field = spec("first\nsecond")
        textarea.value = field.value
        textarea.setSelectionRange(2, 8, "backward")
        measurement.schedule(field, style, 400, Density(1f)) { }
        afterTwoFrames()
        assertEquals(field.value, textarea.value)
        assertEquals(2, textarea.selectionStart)
        assertEquals(8, textarea.selectionEnd)
        assertEquals("backward", textarea.selectionDirection)
    }

    @Test fun disposalCancelsPendingLayoutCallbacks() = runTest {
        var height: Dp? = null
        measurement.schedule(spec("fixture"), style, 400, Density(1f)) { height = it }
        measurement.close()
        afterTwoFrames()
        assertNull(height)
    }

    private fun spec(value: String, maxLines: Int = 5) =
        TextFieldSpec(value, {}, "test.multiline", "Document", singleLine = false, maxLines = maxLines)
}
