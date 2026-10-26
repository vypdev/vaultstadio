package com.vaultstadio.app.accessibility

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import com.vaultstadio.app.feature.auth.AuthError
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertIs

class AuthenticationInteractionTest {
    @get:Rule val rule = createComposeRule()

    @Test fun passwordVisibilityCanBeEnabledAndReversed() {
        val model = inputAuthViewModel()
        rule.setContent { AuthFieldFixture("auth.login.password", model) }
        val field = rule.onNodeWithTag("auth.login.password")
        field.performTextReplacement("test-value")
        rule.onNodeWithContentDescription("Show password").performClick()
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("test-value")))
        rule.onNodeWithContentDescription("Hide password").performClick()
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("•".repeat(10))))
    }

    @Test fun registrationPasswordVisibilityDoesNotRevealConfirmation() {
        val model = inputAuthViewModel()
        rule.setContent { AuthFieldFixture("auth.register.password", model) }
        rule.onNodeWithTag("auth.register.password").performTextReplacement("test-value")
        rule.onNodeWithTag("auth.register.confirm-password").performTextReplacement("test-value")
        rule.onNode(
            hasContentDescription("Show password") and hasAnyAncestor(hasTestTag("auth.register.password")),
            useUnmergedTree = true,
        ).performClick()
        rule.onNodeWithTag("auth.register.password")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("test-value")))
        rule.onNodeWithTag("auth.register.confirm-password")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("•".repeat(10))))
    }

    @Test fun emailImeNextMovesFocusToLoginPassword() {
        val model = inputAuthViewModel()
        rule.setContent { AuthFieldFixture("auth.login.email", model) }
        rule.onNodeWithTag("auth.login.email").performTextReplacement("user@audit.invalid")
        rule.onNodeWithTag("auth.login.email").performImeAction()
        rule.onNodeWithTag("auth.login.password").assertIsFocused()
    }

    @Test fun usernameImeNextMovesFocusToRegistrationPassword() {
        val model = inputAuthViewModel()
        rule.setContent { AuthFieldFixture("auth.register.username", model) }
        rule.onNodeWithTag("auth.register.username").performTextReplacement("Usuario ñ")
        rule.onNodeWithTag("auth.register.username").performImeAction()
        rule.onNodeWithTag("auth.register.password").assertIsFocused()
    }

    @Test fun blankLoginInvokesProductionValidationWithoutAuthentication() {
        val model = inputAuthViewModel()
        rule.setContent { AuthFieldFixture("auth.login.email", model) }
        rule.onNodeWithText("Log in").performClick()
        assertIs<AuthError.EmailPasswordRequired>(model.authError)
    }

    @Test fun mismatchedRegistrationInvokesProductionValidation() {
        val model = inputAuthViewModel()
        rule.setContent { AuthFieldFixture("auth.register.email", model) }
        fillRegistration("different", "mismatch")
        rule.onNodeWithText("Sign up").performClick()
        assertIs<AuthError.PasswordsDoNotMatch>(model.authError)
    }

    @Test fun shortMatchingRegistrationInvokesProductionValidation() {
        val model = inputAuthViewModel()
        rule.setContent { AuthFieldFixture("auth.register.email", model) }
        fillRegistration("short", "short")
        rule.onNodeWithText("Sign up").performClick()
        assertIs<AuthError.PasswordTooShort>(model.authError)
    }

    private fun fillRegistration(password: String, confirmation: String) {
        rule.onNodeWithTag("auth.register.email").performTextReplacement("user@audit.invalid")
        rule.onNodeWithTag("auth.register.username").performTextReplacement("Usuario ñ")
        rule.onNodeWithTag("auth.register.password").performTextReplacement(password)
        rule.onNodeWithTag("auth.register.confirm-password").performTextReplacement(confirmation)
    }
}
