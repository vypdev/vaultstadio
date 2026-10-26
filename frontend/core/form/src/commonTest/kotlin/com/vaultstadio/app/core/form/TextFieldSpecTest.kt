package com.vaultstadio.app.core.form

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TextFieldSpecTest {
    @Test fun blankIdentifierIsRejectedBeforeRendering() {
        assertFailsWith<IllegalArgumentException> { TextFieldSpec("", {}, " ", "Password") }
    }

    @Test fun blankAccessibleNameIsRejectedBeforeRendering() {
        assertFailsWith<IllegalArgumentException> { TextFieldSpec("", {}, "test.password", " ") }
    }

    @Test fun passwordCannotUseAMultilineControl() {
        assertFailsWith<IllegalArgumentException> {
            TextFieldSpec("fixture", {}, "test.password", "Password", isPassword = true, singleLine = false)
        }
    }

    @Test fun ordinaryFieldCannotRequestAnUnprotectedPasswordKeyboard() {
        assertFailsWith<IllegalArgumentException> {
            TextFieldSpec("fixture", {
            }, "test.input", "Input", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        }
    }

    @Test fun inconsistentLineLimitsAreRejectedBeforeRendering() {
        assertFailsWith<IllegalArgumentException> {
            TextFieldSpec("", {}, "test.input", "Input", singleLine = false, minLines = 3, maxLines = 2)
        }
    }
}
