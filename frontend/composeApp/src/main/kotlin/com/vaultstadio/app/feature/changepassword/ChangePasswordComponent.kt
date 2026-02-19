package com.vaultstadio.app.feature.changepassword

import com.arkivanov.decompose.ComponentContext

/**
 * Component for changing user password.
 */
interface ChangePasswordComponent {
    fun onBack()
}

/**
 * Default implementation of ChangePasswordComponent.
 */
class DefaultChangePasswordComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
) : ChangePasswordComponent, ComponentContext by componentContext {

    override fun onBack() = onNavigateBack()
}
