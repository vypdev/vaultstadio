package com.vaultstadio.app.feature.federation

import com.arkivanov.decompose.ComponentContext

interface FederationComponent

class DefaultFederationComponent(
    componentContext: ComponentContext,
) : FederationComponent, ComponentContext by componentContext
