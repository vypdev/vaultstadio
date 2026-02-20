package com.vaultstadio.app.feature.activity

import com.arkivanov.decompose.ComponentContext

interface ActivityComponent

class DefaultActivityComponent(
    componentContext: ComponentContext,
) : ActivityComponent, ComponentContext by componentContext
