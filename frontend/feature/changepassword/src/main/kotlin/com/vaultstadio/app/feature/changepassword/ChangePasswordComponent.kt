package com.vaultstadio.app.feature.changepassword

import com.arkivanov.decompose.ComponentContext

interface ChangePasswordComponent {
    fun onBack()
}

class DefaultChangePasswordComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
) : ChangePasswordComponent, ComponentContext by componentContext {
    override fun onBack() = onNavigateBack()
}
