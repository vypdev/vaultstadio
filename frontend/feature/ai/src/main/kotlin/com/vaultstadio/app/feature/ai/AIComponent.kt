package com.vaultstadio.app.feature.ai

import com.arkivanov.decompose.ComponentContext

interface AIComponent {
    val isAdmin: Boolean
}

class DefaultAIComponent(
    componentContext: ComponentContext,
    override val isAdmin: Boolean = false,
) : AIComponent, ComponentContext by componentContext
