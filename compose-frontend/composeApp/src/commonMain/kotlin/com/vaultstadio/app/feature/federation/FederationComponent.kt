package com.vaultstadio.app.feature.federation

import com.arkivanov.decompose.ComponentContext

/**
 * Component for federation management.
 *
 * With @KoinViewModel, the ViewModel is obtained directly in the Composable
 * using koinViewModel().
 */
interface FederationComponent

/**
 * Default implementation of FederationComponent.
 *
 * Simplified: ViewModel is now injected directly in FederationContent via koinViewModel.
 */
class DefaultFederationComponent(
    componentContext: ComponentContext,
) : FederationComponent, ComponentContext by componentContext
