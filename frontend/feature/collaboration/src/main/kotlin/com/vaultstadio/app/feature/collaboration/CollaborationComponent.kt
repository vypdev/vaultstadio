package com.vaultstadio.app.feature.collaboration

import com.arkivanov.decompose.ComponentContext

interface CollaborationComponent {
    val itemId: String
    val itemName: String
    fun onBack()
}

class DefaultCollaborationComponent(
    componentContext: ComponentContext,
    override val itemId: String,
    override val itemName: String,
    private val onNavigateBack: () -> Unit,
) : CollaborationComponent, ComponentContext by componentContext {

    override fun onBack() = onNavigateBack()
}
