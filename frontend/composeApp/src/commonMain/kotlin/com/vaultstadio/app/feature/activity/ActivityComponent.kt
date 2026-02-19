package com.vaultstadio.app.feature.activity

import com.arkivanov.decompose.ComponentContext

/**
 * Component for displaying recent activity.
 *
 * With @KoinViewModel, the ViewModel is obtained directly in the Composable
 * using koinViewModel().
 */
interface ActivityComponent

/**
 * Default implementation of ActivityComponent.
 *
 * Simplified: ViewModel is now injected directly in ActivityContent via koinViewModel.
 */
class DefaultActivityComponent(
    componentContext: ComponentContext,
) : ActivityComponent, ComponentContext by componentContext
