package com.vaultstadio.app.feature.sharedwithme

import com.arkivanov.decompose.ComponentContext

/**
 * Component for items shared with the current user.
 *
 * The ViewModel is injected directly in SharedWithMeContent
 * using koinViewModel().
 */
interface SharedWithMeComponent {
    fun onBack()
}

/**
 * Simplified: ViewModel is now injected directly in SharedWithMeContent via koinViewModel.
 */
class DefaultSharedWithMeComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
) : SharedWithMeComponent, ComponentContext by componentContext {

    override fun onBack() = onNavigateBack()
}
