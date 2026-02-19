package com.vaultstadio.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.vaultstadio.app.feature.auth.AuthContent
import com.vaultstadio.app.feature.main.MainContent
import com.vaultstadio.app.platform.setPath

/**
 * Root content composable that renders the current child based on navigation state.
 * Syncs URL with the current root screen (auth vs main) for web.
 */
@Composable
fun RootContent(
    component: RootComponent,
    modifier: Modifier = Modifier,
) {
    val childStack by component.stack.subscribeAsState()
    val currentChild = childStack.active.instance

    // Sync URL when on auth screen (web: shows /login)
    LaunchedEffect(currentChild) {
        when (currentChild) {
            is RootComponent.Child.Auth -> setPath(RoutePaths.AUTH_PATH)
            is RootComponent.Child.Main -> { /* MainContent handles main area path */ }
        }
    }

    Children(
        stack = component.stack,
        modifier = modifier,
        animation = stackAnimation(fade()),
    ) { child ->
        when (val instance = child.instance) {
            is RootComponent.Child.Auth -> AuthContent(component = instance.component)
            is RootComponent.Child.Main -> MainContent(component = instance.component)
        }
    }
}
