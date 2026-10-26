package com.vaultstadio.app.accessibility

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertFailsWith

@RunWith(Parameterized::class)
class ProtectedFieldInteractionTest(private val id: String) {
    @get:Rule val rule = createComposeRule()

    @Test fun disabledProductionFieldRejectsEditingAndPreservesState() {
        rule.setContent {
            if (id.startsWith("profile.")) {
                DialogFieldFixture(id, saving = true)
            } else {
                ScreenFieldFixture(id, loading = true)
            }
        }
        val field = rule.onNodeWithTag(id)
        field.assertIsNotEnabled()
        assertFailsWith<AssertionError> { field.performTextReplacement("blocked") }
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.InputText, AnnotatedString("")))
    }

    @Test fun disabledProductionFieldCannotTakeInputFocus() {
        rule.setContent {
            if (id.startsWith("profile.")) {
                DialogFieldFixture(id, saving = true)
            } else {
                ScreenFieldFixture(id, loading = true)
            }
        }
        val field = rule.onNodeWithTag(id)
        field.performClick()
        field.assert(
            SemanticsMatcher("Field is not focused") {
                SemanticsProperties.Focused !in it.config ||
                    !it.config[SemanticsProperties.Focused]
            },
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun fields() = listOf(
            "profile.username",
            "profile.current-password",
            "profile.new-password",
            "profile.confirm-password",
            "password.current",
            "password.new",
            "password.confirm",
        ).map { arrayOf(it) }
    }
}
