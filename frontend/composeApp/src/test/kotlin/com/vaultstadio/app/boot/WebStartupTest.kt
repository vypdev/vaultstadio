package com.vaultstadio.app.boot

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class WebStartupTest {
    @Test fun validConfigurationIsReadOnceAndNativeCanonicalizedBeforeLaunch() = runTest {
        val events = mutableListOf<String>()
        startConfiguredWebApplication("https://ui.invalid", {
            events += "read"
            "{\"apiBaseUrl\":\"https://API.invalid/gateway/api\"}"
        }, {
            events += "native:$it"
            "https://api.invalid/gateway/api"
        }, { events += "launch:$it" }, { error("No failure expected") })
        assertEquals(
            listOf("read", "native:https://API.invalid/gateway/api", "launch:https://api.invalid/gateway/api"),
            events,
        )
    }

    @Test fun invalidJsonCausesNoNativeOrApplicationEffects() = runTest {
        val failures = mutableListOf<WebStartupFailure>()
        startConfiguredWebApplication(
            "https://ui.invalid",
            { "[]" },
            { error("No native parse") },
            { error("No application launch") },
            failures::add,
        )
        assertEquals(listOf(WebStartupFailure.INVALID), failures)
    }

    @Test fun nativeAuthorityRejectionCausesNoApplicationEffects() = runTest {
        val failures = mutableListOf<WebStartupFailure>()
        startConfiguredWebApplication(
            "https://ui.invalid",
            { "{}" },
            { null },
            { error("No application launch") },
            failures::add,
        )
        assertEquals(listOf(WebStartupFailure.INVALID), failures)
    }

    @Test fun loadFailureDoesNotEchoTheProviderOrUseDefault() = runTest {
        val failures = mutableListOf<WebStartupFailure>()
        startConfiguredWebApplication(
            "https://ui.invalid",
            { throw IllegalStateException("fixture-provider-detail") },
            { error("No native parse") },
            { error("No application launch") },
            failures::add,
        )
        assertEquals(listOf(WebStartupFailure.LOAD), failures)
    }

    @Test fun cancellationIsNotAnOrdinaryConfigurationFailure() = runTest {
        assertFailsWith<CancellationException> {
            startConfiguredWebApplication(
                "https://ui.invalid",
                { throw CancellationException("canceled") },
                {
                    error("No native parse")
                },
                { error("No application launch") },
                { error("No failure UI on cancellation") },
            )
        }
    }

    @Test fun fatalFailureIsNotAnOrdinaryConfigurationFailure() = runTest {
        assertFailsWith<AssertionError> {
            startConfiguredWebApplication(
                "https://ui.invalid",
                { throw AssertionError("fatal fixture") },
                { error("No native parse") },
                { error("No application launch") },
                { error("No ordinary failure UI") },
            )
        }
    }

    @Test fun applicationWiringFailureIsNotMisreportedAsBadConfiguration() = runTest {
        assertFailsWith<IllegalStateException> {
            startConfiguredWebApplication(
                "https://ui.invalid",
                { "{}" },
                { it },
                { throw IllegalStateException("wiring fixture") },
                { error("No configuration failure UI") },
            )
        }
    }

    @Test fun applicationDoesNotLaunchBeforeTheConfigurationReadCompletes() = runTest {
        val pending = CompletableDeferred<String>()
        var launched = false
        val boot = async {
            startConfiguredWebApplication("https://ui.invalid", {
                pending.await()
            }, { it }, { launched = true }, { error("No failure") })
        }
        testScheduler.runCurrent()
        assertFalse(launched)
        pending.complete("{}")
        boot.await()
        assertEquals(true, launched)
    }
}
