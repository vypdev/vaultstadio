/**
 * VaultStadio Auth Components
 *
 * Reusable UI components for authentication screens.
 */

package com.vaultstadio.app.ui.screens.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import org.jetbrains.compose.ui.tooling.preview.Preview

private val SampleErrorMessage = "Invalid email or password. Please try again."
private val SampleEmail = "user@example.com"
private val SampleUsername = "johndoe"
private val SamplePassword = "SecurePassword123!"
private val SampleAppName = "VaultStadio"
private val SampleSubtitle = "Your secure storage solution"

/**
 * Error message card for authentication screens.
 */
@Composable
fun AuthErrorCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }
    }
}

/**
 * Email input field.
 */
@Composable
fun EmailTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Email, contentDescription = null)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = imeAction,
        ),
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Username input field.
 */
@Composable
fun UsernameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Person, contentDescription = null)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Password input field with visibility toggle.
 */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showPassword: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Lock, contentDescription = null)
        },
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (showPassword) {
                        Icons.Filled.VisibilityOff
                    } else {
                        Icons.Filled.Visibility
                    },
                    contentDescription = null,
                )
            }
        },
        visualTransformation = if (showPassword) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        singleLine = true,
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
        ),
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * App branding header for auth screens.
 */
@Composable
fun AuthBrandingHeader(
    appName: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Cloud,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = appName,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// region Previews

@Preview
@Composable
internal fun AuthErrorCardPreview() {
    VaultStadioPreview {
        AuthErrorCard(message = SampleErrorMessage)
    }
}

@Preview
@Composable
internal fun EmailTextFieldPreview() {
    VaultStadioPreview {
        EmailTextField(
            value = SampleEmail,
            onValueChange = {},
            label = "Email",
        )
    }
}

@Preview
@Composable
internal fun UsernameTextFieldPreview() {
    VaultStadioPreview {
        UsernameTextField(
            value = SampleUsername,
            onValueChange = {},
            label = "Username",
        )
    }
}

@Preview
@Composable
internal fun PasswordTextFieldPreview() {
    VaultStadioPreview {
        PasswordTextField(
            value = SamplePassword,
            onValueChange = {},
            label = "Password",
            showPassword = false,
            onToggleVisibility = {},
        )
    }
}

@Preview
@Composable
internal fun AuthBrandingHeaderPreview() {
    VaultStadioPreview {
        AuthBrandingHeader(
            appName = SampleAppName,
            subtitle = SampleSubtitle,
        )
    }
}

// endregion
