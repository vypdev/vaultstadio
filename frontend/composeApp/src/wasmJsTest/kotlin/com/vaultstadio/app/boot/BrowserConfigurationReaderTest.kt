@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.boot

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.js.JsAny
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Executes the production Kotlin/Wasm bridge and stream/deadline cleanup in Chrome. */
class BrowserConfigurationReaderTest {
    @Test fun api116ReadsUtf8OnceWithPrivateUncachedFetchOptions() = runTest {
        val fetch = configProbe("ok")
        assertEquals("{\"apiBaseUrl\":\"https://viewer.invalid/é/api\"}", readBrowserConfiguration(fetch))
        assertEquals(1, probeCalls(fetch))
        assertTrue(probeSafeOptions(fetch))
        assertTrue(probeReleased(fetch))
        assertTrue(probeAborted(fetch))
    }

    @Test fun api116RejectsNonJsonOrNonSuccessOrRedirectedDocuments() = runTest {
        for (mode in listOf("status", "mime", "redirect", "missing-stream", "network")) {
            assertFailsWith<WebConfigurationLoadException> { readBrowserConfiguration(configProbe(mode)) }
        }
    }

    @Test fun api116ActualByteBudgetCannotBeBypassedByHeaders() = runTest {
        for (mode in listOf("oversize", "lying-length", "bad-length", "large-length", "invalid-utf8", "empty-flood")) {
            val fetch = configProbe(mode)
            assertFailsWith<WebConfigurationLoadException> { readBrowserConfiguration(fetch) }
            assertTrue(probeReleased(fetch))
            assertTrue(probeAborted(fetch))
        }
    }

    @Test fun api116ExactByteBudgetAndMissingLengthAreValid() = runTest {
        for (mode in listOf("boundary", "absent-length")) {
            val fetch = configProbe(mode)
            val value = readBrowserConfiguration(fetch)
            assertEquals(if (mode == "boundary") 16384 else 2, value.length)
            assertTrue(probeReleased(fetch))
        }
    }

    @Test fun api116TotalDeadlineAbortsHungHeadersAndBody() = runTest {
        for (mode in listOf("hung-fetch", "hung-body")) {
            val fetch = configProbe(mode)
            assertFailsWith<WebConfigurationLoadException> { readBrowserConfiguration(fetch, deadlineMs = 20) }
            assertTrue(probeAborted(fetch))
            if (mode == "hung-body") assertTrue(probeReleased(fetch))
        }
    }

    @Test fun api116CancellationAbortsWithoutLaunchingOrReportingConfigurationSuccess() = runTest {
        val fetch = configProbe("hung-fetch")
        val pending = async(start = CoroutineStart.UNDISPATCHED) { readBrowserConfiguration(fetch) }
        pending.cancelAndJoin()
        assertTrue(probeAborted(fetch))
        assertTrue(pending.isCancelled)
    }

    @Test fun api116CancellationReleasesAnAcquiredBodyReader() = runTest {
        val fetch = configProbe("hung-body")
        val pending = async(start = CoroutineStart.UNDISPATCHED) { readBrowserConfiguration(fetch) }
        awaitConfigurationTurn()
        pending.cancelAndJoin()
        assertTrue(probeAborted(fetch))
        assertTrue(probeReleased(fetch))
        assertTrue(probeCancelled(fetch))
        assertTrue(pending.isCancelled)
    }

    @Test fun api116ResponseArrivingAfterCancellationIsCancelledWithoutReading() = runTest {
        val fetch = configProbe("late-fetch")
        val pending = async(start = CoroutineStart.UNDISPATCHED) { readBrowserConfiguration(fetch) }
        pending.cancelAndJoin()
        deliverLateConfiguration(fetch)
        awaitConfigurationTurn()
        assertTrue(probeCancelled(fetch))
        assertTrue(probeAborted(fetch))
        assertEquals(0, probeReads(fetch))
        assertTrue(pending.isCancelled)
    }

    @Test fun api116MissingFetchFailsSafely() = runTest {
        assertFailsWith<WebConfigurationLoadException> { readBrowserConfiguration(null) }
    }

    @Test fun api116NativeParserRejectsMalformedHostsAndCanonicalizesValidOnes() {
        assertNull(canonicalBrowserApiUrl("http://999.999.999.999/api"))
        assertNull(canonicalBrowserApiUrl("https://[xyz]/api"))
        assertEquals("https://xn--bcher-kva.invalid/api", canonicalBrowserApiUrl("https://bücher.invalid/api"))
        assertEquals("https://viewer.invalid/api", canonicalBrowserApiUrl("https://VIEWER.invalid:443/api"))
    }
}

private fun configProbe(mode: String): JsAny = js(
    """(function() {
    const state = { calls: 0, options: null, url: null, released: false, aborted: false, cancelled: false, reads: 0 };
    const fetch = function(url, options) {
        state.calls++; state.url=url; state.options=options;
        options.signal.addEventListener('abort', () => {state.aborted=true;});
        if (mode==='hung-fetch') return new Promise(() => {});
        if (mode==='network') return Promise.reject(Error('private URL must not appear in safe failure'));
        const text = mode==='ok' ? '{"apiBaseUrl":"https://viewer.invalid/é/api"}' : '{}';
        let bytes = new TextEncoder().encode(text);
        if (mode==='boundary') bytes=new Uint8Array(16384).fill(32);
        if (mode==='oversize' || mode==='lying-length') bytes=new Uint8Array(16385).fill(32);
        if (mode==='invalid-utf8') bytes=new Uint8Array([0xc3,0x28]);
        let index=0;
        const reader={
            read() {
                state.reads++;
                if(mode==='hung-body') return new Promise(() => {});
                if(mode==='empty-flood') return Promise.resolve({done:false,value:new Uint8Array()});
                return Promise.resolve(index++===0 ? {done:false,value:bytes} : {done:true});
            },
            cancel() { state.cancelled=true;return Promise.resolve(); }, releaseLock() {state.released=true;},
        };
        const response={
            ok:mode!=='status',redirected:mode==='redirect',
            headers: {get(name) {if(name==='Content-Type')return mode==='mime'?'text/html':'application/json; charset=utf-8';
                return mode==='absent-length'||mode==='oversize'||mode==='empty-flood'?null:
                    mode==='lying-length'?'2':mode==='bad-length'?'invalid':mode==='large-length'?'16385':String(bytes.length);}},
            body:mode==='missing-stream'?null:{getReader() {return reader;},cancel() {state.cancelled=true;return Promise.resolve();}},
        };
        if(mode==='late-fetch') return new Promise(resolve => {state.deliver=()=>resolve(response);});
        return Promise.resolve(response);
    };
    fetch.state=state;return fetch;
})()""",
)
private fun probeCalls(fetch: JsAny): Int = js("fetch.state.calls")
private fun probeReleased(fetch: JsAny): Boolean = js("fetch.state.released")
private fun probeAborted(fetch: JsAny): Boolean = js("fetch.state.aborted")
private fun probeSafeOptions(
    fetch: JsAny,
): Boolean = js(
    "fetch.state.url==='/vaultstadio-config.json'&&fetch.state.options.credentials==='omit'&&fetch.state.options.cache==='no-store'&&fetch.state.options.redirect==='error'",
)

private fun probeCancelled(fetch: JsAny): Boolean = js("fetch.state.cancelled")
private fun probeReads(fetch: JsAny): Int = js("fetch.state.reads")
private fun deliverLateConfiguration(fetch: JsAny): Unit = js("fetch.state.deliver()")
private suspend fun awaitConfigurationTurn(): Unit = suspendCoroutine { continuation ->
    configurationTurn { continuation.resume(Unit) }
}
private fun configurationTurn(done: () -> Unit): Unit = js("{setTimeout(done,0);}")
