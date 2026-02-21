package com.vaultstadio.app.feature.versionhistory

import com.arkivanov.decompose.ComponentContext

interface VersionHistoryComponent {
    val itemId: String
    val itemName: String
    fun onBack()
}

class DefaultVersionHistoryComponent(
    componentContext: ComponentContext,
    override val itemId: String,
    override val itemName: String,
    private val onNavigateBack: () -> Unit,
) : VersionHistoryComponent, ComponentContext by componentContext {

    override fun onBack() = onNavigateBack()
}
