package com.vaultstadio.app.feature.versionhistory

import com.arkivanov.decompose.ComponentContext

/**
 * Component for version history.
 *
 * The ViewModel is injected directly in VersionHistoryContent
 * using koinViewModel { parametersOf(itemId) }.
 */
interface VersionHistoryComponent {
    val itemId: String
    val itemName: String
    fun onBack()
}

/**
 * Simplified: ViewModel is now injected directly in VersionHistoryContent via koinViewModel.
 */
class DefaultVersionHistoryComponent(
    componentContext: ComponentContext,
    override val itemId: String,
    override val itemName: String,
    private val onNavigateBack: () -> Unit,
) : VersionHistoryComponent, ComponentContext by componentContext {

    override fun onBack() = onNavigateBack()
}
