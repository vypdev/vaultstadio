package com.vaultstadio.app.feature.shares

import com.arkivanov.decompose.ComponentContext

interface SharesComponent

class DefaultSharesComponent(
    componentContext: ComponentContext,
) : SharesComponent, ComponentContext by componentContext
