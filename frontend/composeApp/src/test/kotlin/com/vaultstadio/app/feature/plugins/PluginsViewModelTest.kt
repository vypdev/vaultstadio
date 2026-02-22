/**
 * Unit tests for PluginsViewModel: clearError and loadPlugins result.
 * Async enable/disable are covered by use case tests.
 */

package com.vaultstadio.app.feature.plugins

import com.vaultstadio.app.domain.plugin.model.PluginInfo
import com.vaultstadio.app.domain.plugin.usecase.DisablePluginUseCase
import com.vaultstadio.app.domain.plugin.usecase.EnablePluginUseCase
import com.vaultstadio.app.domain.plugin.usecase.GetPluginsUseCase
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.feature.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun loadPlugins_success_setsPlugins() = ViewModelTestBase.runTestWithMain {
        val list = listOf(testPlugin("p1"), testPlugin("p2"))
        val vm = createViewModel(getPluginsResult = Result.success(list))
        vm.loadPlugins()
        assertEquals(2, vm.plugins.size)
        assertEquals("p1", vm.plugins[0].id)
        assertNull(vm.error)
    }

    @Test
    fun loadPlugins_error_setsError() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getPluginsResult = Result.error("FORBIDDEN", "Access denied"))
        vm.loadPlugins()
        assertTrue(vm.plugins.isEmpty())
        assertEquals("Access denied", vm.error)
    }
}
