package com.vaultstadio.app.accessibility

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.core.resources.EnglishStrings
import com.vaultstadio.app.feature.auth.AuthError
import com.vaultstadio.app.feature.auth.AuthScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class AuthResponsiveInteractionTest(
    private val width: Int,
    private val fontScale: Float,
    private val registration: Boolean,
) {
    @get:Rule val rule = createComposeRule()

    @Test fun productionFieldsAndActionsFitAndRetainEditingAndValidation() {
        val model = inputAuthViewModel()
        if (registration) model.toggleRegister()
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                MaterialTheme {
                    Box(Modifier.width(width.dp).testTag("auth-viewport")) { AuthScreen(model) }
                }
            }
        }
        val viewport = rule.onNodeWithTag("auth-viewport").getUnclippedBoundsInRoot()
        assertEquals(width.dp, viewport.right - viewport.left, "Fixture must provide the requested actual viewport")
        val prefix = if (registration) "auth.register" else "auth.login"
        val fields = if (registration) {
            listOf("email", "username", "password", "confirm-password")
        } else {
            listOf("email", "password")
        }
        fields.forEach { assertFits(rule.onNodeWithTag("$prefix.$it"), viewport) }
        val submit = rule.onNode(
            hasText(if (registration) EnglishStrings.authRegister else EnglishStrings.authLogin) and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
        )
        assertFits(submit, viewport)
        assertFits(
            rule.onNodeWithText(if (registration) EnglishStrings.authHaveAccount else EnglishStrings.authNoAccount),
            viewport,
        )
        val email = rule.onNodeWithTag("$prefix.email")
        email.performTextReplacement("usuario+ñ@audit.invalid")
        assertEquals("usuario+ñ@audit.invalid", if (registration) model.registerEmail else model.loginEmail)
        email.performTextReplacement("")
        submit.performClick()
        assertEquals(
            if (registration) AuthError.AllFieldsRequired else AuthError.EmailPasswordRequired,
            model.authError,
        )
    }

    private fun assertFits(node: SemanticsNodeInteraction, viewport: DpRect) {
        val bounds = node.getUnclippedBoundsInRoot()
        val controlWidth = bounds.right - bounds.left
        assertTrue(controlWidth > 0.dp, "Control must have usable width: $bounds")
        assertTrue(bounds.left >= viewport.left, "Control overflows left: $bounds outside $viewport")
        assertTrue(bounds.right <= viewport.right, "Control overflows right: $bounds outside $viewport")
        assertTrue(controlWidth <= 400.dp, "Wide viewport must retain a bounded form: $bounds")
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "width={0}, fontScale={1}, registration={2}")
        fun layouts() = (listOf(280, 320, 360, 400, 800).map { it to 1f } + (320 to 1.5f))
            .flatMap { (width, scale) -> listOf(false, true).map { arrayOf<Any>(width, scale, it) } }
    }
}
