package com.vaultstadio.app.feature.ai

import com.arkivanov.decompose.ComponentContext

/**
 * Component for AI features.
 *
 * With @KoinViewModel, the ViewModel is obtained directly in the Composable
 * using koinViewModel().
 */
interface AIComponent {
    val isAdmin: Boolean
}

/**
 * Default implementation of AIComponent.
 *
 * Simplified: ViewModel is now injected directly in AIContent via koinViewModel.
 */
class DefaultAIComponent(
    componentContext: ComponentContext,
    override val isAdmin: Boolean = false,
) : AIComponent, ComponentContext by componentContext
