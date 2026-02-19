/**
 * Helpers for ViewModel and UploadManager tests that use viewModelScope (Main dispatcher).
 * Set Dispatchers.Main to UnconfinedTestDispatcher so tests pass on desktop and Android
 * without requiring a real main looper. Use withMainDispatcher() or runTestWithMain() in each test.
 */

package com.vaultstadio.app.feature

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
object ViewModelTestBase {

    private val testDispatcher = UnconfinedTestDispatcher()

    /** Run a synchronous test with Main set to UnconfinedTestDispatcher; resets Main after. */
    fun withMainDispatcher(block: () -> Unit) {
        Dispatchers.setMain(testDispatcher)
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    /** Run a coroutine test with Main set to UnconfinedTestDispatcher; resets Main after. */
    fun runTestWithMain(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(testDispatcher)
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }
}
