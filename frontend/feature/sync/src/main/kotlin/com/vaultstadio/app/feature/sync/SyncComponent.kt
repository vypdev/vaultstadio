package com.vaultstadio.app.feature.sync

import com.arkivanov.decompose.ComponentContext

interface SyncComponent

class DefaultSyncComponent(
    componentContext: ComponentContext,
) : SyncComponent, ComponentContext by componentContext
