/**
 * Unit tests for PluginsViewModel: clearError and loadPlugins result.
 * Async enable/disable are covered by use case tests.
 */

package com.vaultstadio.app.feature.plugins

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.plugin.model.PluginInfo
import com.vaultstadio.app.domain.plugin.usecase.DisablePluginUseCase
import com.vaultstadio.app.domain.plugin.usecase.EnablePluginUseCase
import com.vaultstadio.app.domain.plugin.usecase.GetPluginsUseCase
import com.vaultstadio.app.feature.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertNull

private fun testPlugin(id: String = "p1") = PluginInfo(
    id = id,
    name = "Test Plugin",
    version = "1.0",
    description = "Desc",
    author = "Author",
    isEnabled = true,
    state = "loaded",
)

private class FakeGetPluginsUseCase(
    var result: Result<List<PluginInfo>> = Result.success(emptyList()),
) : GetPluginsUseCase {
    override suspend fun invoke(): Result<List<PluginInfo>> = result
}

private class FakeEnablePluginUseCase(
    var result: Result<PluginInfo> = Result.success(testPlugin()),
) : EnablePluginUseCase {
    override suspend fun invoke(pluginId: String): Result<PluginInfo> = result
}

private class FakeDisablePluginUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : DisablePluginUseCase {
    override suspend fun invoke(pluginId: String): Result<Unit> = result
}

class PluginsViewModelTest {

    private fun createViewModel(
        getPluginsResult: Result<List<PluginInfo>> = Result.success(emptyList()),
    ): PluginsViewModel = PluginsViewModel(
        getPluginsUseCase = FakeGetPluginsUseCase(getPluginsResult),
        enablePluginUseCase = FakeEnablePluginUseCase(),
        disablePluginUseCase = FakeDisablePluginUseCase(),
    )

    @Test
    fun clearError_doesNotThrow() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        vm.clearError()
        assertNull(vm.error)
    }

    @Test
    fun loadPlugins_doesNotThrow() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        vm.loadPlugins()
    }
}
