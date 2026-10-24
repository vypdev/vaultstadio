package com.vaultstadio.app.feature.sharedwithme

import com.arkivanov.decompose.ComponentContext

interface SharedWithMeComponent {
    fun onBack()
}

class DefaultSharedWithMeComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
) : SharedWithMeComponent, ComponentContext by componentContext {

    override fun onBack() = onNavigateBack()
}
