/**
 * Unit tests for plugin use cases (GetPlugins, EnablePlugin, DisablePlugin).
 * Uses a fake PluginRepository to avoid platform/DI.
 */

package com.vaultstadio.app.domain.usecase.plugin

import com.vaultstadio.app.domain.plugin.PluginRepository
import com.vaultstadio.app.domain.plugin.model.PluginInfo
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.plugin.usecase.DisablePluginUseCaseImpl
import com.vaultstadio.app.data.plugin.usecase.EnablePluginUseCaseImpl
import com.vaultstadio.app.data.plugin.usecase.GetPluginsUseCaseImpl
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun testPluginInfo(
    id: String = "plugin-1",
    name: String = "Image Metadata",
    isEnabled: Boolean = false,
) = PluginInfo(
    id = id,
    name = name,
    version = "1.0.0",
    description = "Extracts image metadata",
    author = "VaultStadio",
    isEnabled = isEnabled,
    state = "loaded",
)

private class FakePluginRepository(
    var getPluginsResult: Result<List<PluginInfo>> = Result.success(emptyList()),
    var enablePluginResult: Result<PluginInfo> = Result.success(testPluginInfo(isEnabled = true)),
    var disablePluginResult: Result<Unit> = Result.success(Unit),
) : PluginRepository {

    override suspend fun getPlugins(): Result<List<PluginInfo>> = getPluginsResult

    override suspend fun enablePlugin(pluginId: String): Result<PluginInfo> = enablePluginResult

    override suspend fun disablePlugin(pluginId: String): Result<Unit> = disablePluginResult
}

class GetPluginsUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetPluginsResult() = runTest {
        val plugins = listOf(testPluginInfo("p1"), testPluginInfo("p2"))
        val repo = FakePluginRepository(getPluginsResult = Result.success(plugins))
        val useCase = GetPluginsUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.size)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakePluginRepository(getPluginsResult = Result.error("FORBIDDEN", "Admin only"))
        val useCase = GetPluginsUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class EnablePluginUseCaseTest {

    @Test
    fun invoke_returnsRepositoryEnablePluginResult() = runTest {
        val plugin = testPluginInfo(id = "p1", isEnabled = true)
        val repo = FakePluginRepository(enablePluginResult = Result.success(plugin))
        val useCase = EnablePluginUseCaseImpl(repo)
        val result = useCase("p1")
        assertTrue(result.isSuccess())
        assertEquals(plugin, result.getOrNull())
        assertTrue(result.getOrNull()?.isEnabled == true)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakePluginRepository(enablePluginResult = Result.error("NOT_FOUND", "Plugin not found"))
        val useCase = EnablePluginUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
    }
}

class DisablePluginUseCaseTest {

    @Test
    fun invoke_returnsRepositoryDisablePluginResult() = runTest {
        val repo = FakePluginRepository(disablePluginResult = Result.success(Unit))
        val useCase = DisablePluginUseCaseImpl(repo)
        val result = useCase("plugin-1")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakePluginRepository(disablePluginResult = Result.error("CONFLICT", "Plugin in use"))
        val useCase = DisablePluginUseCaseImpl(repo)
        val result = useCase("p1")
        assertTrue(result.isError())
    }
}
