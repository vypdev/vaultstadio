package com.vaultstadio.app.accessibility

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaultstadio.app.core.resources.EnglishStrings
import com.vaultstadio.app.feature.auth.AuthScreen
import org.junit.Rule
import org.junit.Test

class AuthScreenInteractionTest {
    @get:Rule val rule = createComposeRule()

    @Test fun modeSwitchRendersTheActualRequestedForm() {
        val model = inputAuthViewModel()
        rule.setContent { AuthScreen(model) }
        rule.onNodeWithTag("auth.login.email").assertExists()
        rule.onNodeWithTag("auth.register.email").assertDoesNotExist()
        rule.onNodeWithText(EnglishStrings.authNoAccount).performClick()
        rule.onNodeWithTag("auth.register.email").assertExists()
        rule.onNodeWithTag("auth.login.email").assertDoesNotExist()
        rule.onNodeWithText(EnglishStrings.authHaveAccount).performClick()
        rule.onNodeWithTag("auth.login.email").assertExists()
    }

    @Test fun productionValidationRendersAnErrorAndModeSwitchClearsIt() {
        val model = inputAuthViewModel()
        rule.setContent { AuthScreen(model) }
        rule.onNode(
            hasText(EnglishStrings.authLogin) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
        ).performClick()
        rule.onNodeWithText(EnglishStrings.errorEmailPasswordRequired).assertExists()
        rule.onNodeWithText(EnglishStrings.authNoAccount).performClick()
        rule.onNodeWithText(EnglishStrings.errorEmailPasswordRequired).assertDoesNotExist()
    }
}
