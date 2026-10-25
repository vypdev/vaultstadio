package com.vaultstadio.app.feature.licenses

import com.arkivanov.decompose.ComponentContext

interface LicensesComponent {
    fun onBack()
}

class DefaultLicensesComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
) : LicensesComponent, ComponentContext by componentContext {
    override fun onBack() = onNavigateBack()
}
