package com.vaultstadio.app.feature.licenses

import com.arkivanov.decompose.ComponentContext

/**
 * Component for displaying open source licenses.
 */
interface LicensesComponent {
    fun onBack()
}

/**
 * Default implementation of LicensesComponent.
 */
class DefaultLicensesComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
) : LicensesComponent, ComponentContext by componentContext {

    override fun onBack() = onNavigateBack()
}
