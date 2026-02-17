package com.vaultstadio.app.feature.collaboration

import com.arkivanov.decompose.ComponentContext

/**
 * Component for real-time collaboration.
 *
 * The ViewModel is injected directly in CollaborationContent
 * using koinViewModel { parametersOf(itemId) }.
 */
interface CollaborationComponent {
    val itemId: String
    val itemName: String
    fun onBack()
}

/**
 * Simplified: ViewModel is now injected directly in CollaborationContent via koinViewModel.
 */
class DefaultCollaborationComponent(
    componentContext: ComponentContext,
    override val itemId: String,
    override val itemName: String,
    private val onNavigateBack: () -> Unit,
) : CollaborationComponent, ComponentContext by componentContext {

    override fun onBack() = onNavigateBack()
}
