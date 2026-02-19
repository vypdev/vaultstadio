package com.vaultstadio.app.feature.shares

import com.arkivanov.decompose.ComponentContext

/**
 * Component for managing user's shared links.
 *
 * With @KoinViewModel, the ViewModel is obtained directly in the Composable
 * using koinViewModel().
 */
interface SharesComponent

/**
 * Default implementation of SharesComponent.
 *
 * Simplified: ViewModel is now injected directly in SharesContent via koinViewModel.
 */
class DefaultSharesComponent(
    componentContext: ComponentContext,
) : SharesComponent, ComponentContext by componentContext
