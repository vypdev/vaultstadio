package com.vaultstadio.app.feature.profile

import com.arkivanov.decompose.ComponentContext

interface ProfileComponent {
    fun onBack()
    fun navigateToChangePassword()
    fun navigateToSecurity()
    fun exportData(fileName: String, data: ByteArray, mimeType: String)
}

class DefaultProfileComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
    private val onNavigateToChangePassword: () -> Unit = {},
    private val onNavigateToSecurity: () -> Unit = {},
    private val onExportData: (String, ByteArray, String) -> Unit = { _, _, _ -> },
) : ProfileComponent, ComponentContext by componentContext {
    override fun onBack() = onNavigateBack()
    override fun navigateToChangePassword() = onNavigateToChangePassword()
    override fun navigateToSecurity() = onNavigateToSecurity()
    override fun exportData(fileName: String, data: ByteArray, mimeType: String) = onExportData(fileName, data, mimeType)
}
