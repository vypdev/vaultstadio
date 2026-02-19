/**
 * VaultStadio Plugin Lifecycle Tests
 *
 * Unit tests for PluginLifecycle interface default behavior (via concrete implementation).
 */

package com.vaultstadio.plugins.lifecycle

import com.vaultstadio.plugins.api.AbstractPlugin
import com.vaultstadio.plugins.api.PluginMetadata
import com.vaultstadio.plugins.context.AIApi
import com.vaultstadio.plugins.context.HttpClientApi
import com.vaultstadio.plugins.context.PluginContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

class PluginLifecycleTest {

    @Test
    fun `lifecycle implementation receives onLoad then onInitialize then onShutdown`() = runTest {
        val recorder = LifecycleRecorderPlugin()
        val context = mockContext()
        recorder.onLoad(context)
        assertEquals(listOf("load"), recorder.calls)
        recorder.onInitialize(context)
        assertEquals(listOf("load", "initialize"), recorder.calls)
        recorder.onShutdown()
        assertEquals(listOf("load", "initialize", "shutdown"), recorder.calls)
    }

    @Test
    fun `onUnload onUpgrade onEnable onDisable are callable with defaults`() = runTest {
        val recorder = LifecycleRecorderPlugin()
        val context = mockContext()
        recorder.onUnload()
        recorder.onUpgrade("0.9", context)
        recorder.onEnable(context)
        recorder.onDisable()
        // Default implementations do nothing; just ensure no throw
        assertEquals(0, recorder.calls.size)
    }

    private fun mockContext(): PluginContext = object : PluginContext {
        override val pluginId: String get() = "test"
        override val scope: CoroutineScope get() = CoroutineScope(EmptyCoroutineContext)
        override val eventBus get() = throw UnsupportedOperationException("mock")
        override val storage get() = throw UnsupportedOperationException("mock")
        override val metadata get() = throw UnsupportedOperationException("mock")
        override val users get() = throw UnsupportedOperationException("mock")
        override val logger get() = throw UnsupportedOperationException("mock")
        override val config get() = throw UnsupportedOperationException("mock")
        override val tempDirectory get() = throw UnsupportedOperationException("mock")
        override val dataDirectory get() = throw UnsupportedOperationException("mock")
        override val httpClient: HttpClientApi? get() = null
        override val ai: AIApi? get() = null
        override fun registerEndpoint(
            method: String,
            path: String,
            handler: suspend (
                com.vaultstadio.plugins.context.EndpointRequest,
            ) -> com.vaultstadio.plugins.context.EndpointResponse,
        ) {}
        override fun unregisterEndpoint(method: String, path: String) {}
        override suspend fun scheduleTask(
            name: String,
            cronExpression: String?,
            task: suspend () -> Unit,
        ): String = "task-id"
        override suspend fun cancelTask(taskId: String) {}
    }

    private class LifecycleRecorderPlugin : AbstractPlugin() {
        val calls = mutableListOf<String>()
        override val metadata = PluginMetadata(
            id = "lifecycle-test",
            name = "Lifecycle Test",
            version = "1.0",
            description = "Test",
            author = "Test",
        )
        override suspend fun onLoad(context: PluginContext) {
            super.onLoad(context)
            calls.add("load")
        }
        override suspend fun onInitialize(context: PluginContext) {
            super.onInitialize(context)
            calls.add("initialize")
        }
        override suspend fun onShutdown() {
            calls.add("shutdown")
            super.onShutdown()
        }
    }
}
