package com.vaultstadio.app.accessibility

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.vaultstadio.app.core.resources.ChineseStrings
import com.vaultstadio.app.core.resources.EnglishStrings
import com.vaultstadio.app.core.resources.FrenchStrings
import com.vaultstadio.app.core.resources.GermanStrings
import com.vaultstadio.app.core.resources.InputLabel
import com.vaultstadio.app.core.resources.JapaneseStrings
import com.vaultstadio.app.core.resources.Language
import com.vaultstadio.app.core.resources.LocalStrings
import com.vaultstadio.app.core.resources.PortugueseStrings
import com.vaultstadio.app.core.resources.SpanishStrings
import com.vaultstadio.app.core.resources.inputLabel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class LocalizedInputInteractionTest(private val language: Language) {
    @get:Rule val rule = createComposeRule()

    @Test fun realInputAndVisibilityActionUseCurrentLocale() {
        val resources = when (language) {
            Language.ENGLISH -> EnglishStrings
            Language.SPANISH -> SpanishStrings
            Language.FRENCH -> FrenchStrings
            Language.GERMAN -> GermanStrings
            Language.PORTUGUESE -> PortugueseStrings
            Language.CHINESE -> ChineseStrings
            Language.JAPANESE -> JapaneseStrings
        }
        val model = inputAuthViewModel()
        rule.setContent {
            CompositionLocalProvider(LocalStrings provides resources) {
                AuthFieldFixture("auth.login.email", model)
            }
        }
        rule.onNodeWithTag("auth.login.email").assertContentDescriptionEquals(resources.authEmail)
        rule.onNodeWithTag("auth.login.password").assertContentDescriptionEquals(resources.authPassword)
        rule.onNodeWithContentDescription(resources.inputLabel(InputLabel.SHOW_PASSWORD)).performClick()
        rule.onNodeWithContentDescription(resources.inputLabel(InputLabel.HIDE_PASSWORD)).assertExists()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun languages() = Language.entries.map { arrayOf(it) }
    }
}
