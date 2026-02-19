package com.vaultstadio.app.feature.plugins

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaultstadio.app.ui.screens.PluginsScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * Plugins feature content - injects ViewModel and delegates to PluginsScreen.
 */
@Composable
fun PluginsContent(
    component: PluginsComponent,
    modifier: Modifier = Modifier,
) {
    val viewModel: PluginsViewModel = koinViewModel()

    PluginsScreen(
        plugins = viewModel.plugins,
        isLoading = viewModel.isLoading,
        error = viewModel.error,
        onRefresh = viewModel::loadPlugins,
        onEnablePlugin = viewModel::enablePlugin,
        onDisablePlugin = viewModel::disablePlugin,
        onClearError = viewModel::clearError,
        onNavigateBack = component::onBack,
        modifier = modifier,
    )
}
