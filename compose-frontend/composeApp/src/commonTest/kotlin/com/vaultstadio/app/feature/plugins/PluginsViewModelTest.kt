/**
 * Unit tests for PluginsViewModel: clearError and loadPlugins result.
 * Async enable/disable are covered by use case tests.
 */

package com.vaultstadio.app.feature.plugins

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.model.PluginInfo
import com.vaultstadio.app.domain.usecase.plugin.DisablePluginUseCase
import com.vaultstadio.app.domain.usecase.plugin.EnablePluginUseCase
import com.vaultstadio.app.domain.usecase.plugin.GetPluginsUseCase
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
    var result: ApiResult<List<PluginInfo>> = ApiResult.success(emptyList()),
) : GetPluginsUseCase {
    override suspend fun invoke(): ApiResult<List<PluginInfo>> = result
}

private class FakeEnablePluginUseCase(
    var result: ApiResult<PluginInfo> = ApiResult.success(testPlugin()),
) : EnablePluginUseCase {
    override suspend fun invoke(pluginId: String): ApiResult<PluginInfo> = result
}

private class FakeDisablePluginUseCase(
    var result: ApiResult<Unit> = ApiResult.success(Unit),
) : DisablePluginUseCase {
    override suspend fun invoke(pluginId: String): ApiResult<Unit> = result
}

class PluginsViewModelTest {

    private fun createViewModel(
        getPluginsResult: ApiResult<List<PluginInfo>> = ApiResult.success(emptyList()),
    ): PluginsViewModel = PluginsViewModel(
        getPluginsUseCase = FakeGetPluginsUseCase(getPluginsResult),
        enablePluginUseCase = FakeEnablePluginUseCase(),
        disablePluginUseCase = FakeDisablePluginUseCase(),
    )

    @Test
    fun clearError_doesNotThrow() {
        val vm = createViewModel()
        vm.clearError()
        assertNull(vm.error)
    }

    @Test
    fun loadPlugins_doesNotThrow() {
        val vm = createViewModel()
        vm.loadPlugins()
    }
}
