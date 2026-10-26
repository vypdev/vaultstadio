package com.vaultstadio.app.core.form

import kotlin.test.Test
import kotlin.test.assertFailsWith

class PasswordFieldSpecTest {
    @Test fun blankIdentifierIsRejectedBeforeRendering() {
        assertFailsWith<IllegalArgumentException> { PasswordFieldSpec("", {}, " ", "Password") }
    }

    @Test fun blankAccessibleNameIsRejectedBeforeRendering() {
        assertFailsWith<IllegalArgumentException> { PasswordFieldSpec("", {}, "test.password", " ") }
    }
}
