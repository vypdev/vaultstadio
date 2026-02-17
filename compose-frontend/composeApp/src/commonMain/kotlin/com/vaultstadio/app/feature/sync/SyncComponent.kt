package com.vaultstadio.app.feature.sync

import com.arkivanov.decompose.ComponentContext

/**
 * Component for device synchronization management.
 *
 * With @KoinViewModel, the ViewModel is obtained directly in the Composable
 * using koinViewModel().
 */
interface SyncComponent

/**
 * Default implementation of SyncComponent.
 *
 * Simplified: ViewModel is now injected directly in SyncContent via koinViewModel.
 */
class DefaultSyncComponent(
    componentContext: ComponentContext,
) : SyncComponent, ComponentContext by componentContext
