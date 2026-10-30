/**
 * Authentication screen content (Login/Register).
 */

package com.vaultstadio.app.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AuthContent(
    component: AuthComponent,
    modifier: Modifier = Modifier,
) {
    val viewModel: AuthViewModel = koinViewModel {
        parametersOf(AuthSuccessCallback { component.onAuthSuccess() })
    }
    AuthScreen(viewModel, modifier)
}
