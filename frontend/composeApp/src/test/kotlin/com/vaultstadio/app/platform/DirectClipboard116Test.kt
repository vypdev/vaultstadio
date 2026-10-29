package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.share.ClipboardWriteOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DirectClipboard116Test {
    @Test fun api116NativeAdapterCallsTheOutputWithExactText() = runTest {
        val writes = mutableListOf<String>()
        val result = DirectShareLinkClipboard {
            writes += it
        }.write("https://viewer.invalid/gateway/api/v1/share/CaseToken/view")
        assertEquals(ClipboardWriteOutcome.COPIED, result)
        assertEquals(listOf("https://viewer.invalid/gateway/api/v1/share/CaseToken/view"), writes)
    }

    @Test fun api116NativeRefusalIsNotACopiedOutcome() = runTest {
        assertEquals(
            ClipboardWriteOutcome.FAILED,
            DirectShareLinkClipboard {
                error("platform unavailable")
            }.write("link"),
        )
    }

    @Test fun api116NativeCancellationEscapes() = runTest {
        assertFailsWith<CancellationException> {
            DirectShareLinkClipboard { throw CancellationException("cancel") }.write("link")
        }
    }

    @Test fun api116NativeFatalErrorIsNotMisreportedAsClipboardRefusal() = runTest {
        assertFailsWith<AssertionError> { DirectShareLinkClipboard { throw AssertionError("fatal") }.write("link") }
    }
}
