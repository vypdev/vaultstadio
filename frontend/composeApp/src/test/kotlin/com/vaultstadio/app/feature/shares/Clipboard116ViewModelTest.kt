@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.vaultstadio.app.feature.shares

import com.vaultstadio.app.domain.share.ClipboardWriteOutcome
import com.vaultstadio.app.domain.share.ShareLinkClipboard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Clipboard116ViewModelTest {
    @Test fun api116CopyRequiresActualAcknowledgmentAndUsesTheConfiguredLocation() = clipboard116Test {
        val ack = CompletableDeferred<ClipboardWriteOutcome>()
        val writes = mutableListOf<String>()
        val vm = clipboard116Model(
            ShareLinkClipboard {
                writes += it
                ack.await()
            },
        )
        vm.copyLink(clipboard116Share())
        assertEquals(listOf("https://viewer.invalid/gateway/api/v1/share/CaseToken_-/view"), writes)
        assertTrue(vm.isCopyingLink)
        assertNull(vm.clipboardLink)
        assertNull(vm.copyError)
        ack.complete(ClipboardWriteOutcome.COPIED)
        runCurrent()
        assertEquals(writes.single(), vm.clipboardLink)
        assertFalse(vm.isCopyingLink)
        vm.clearClipboardLink()
        assertNull(vm.clipboardLink)
    }

    @Test fun api116UnavailableDeniedAndFailedAreNeverReportedCopied() = clipboard116Test {
        for (outcome in listOf(
            ClipboardWriteOutcome.UNAVAILABLE,
            ClipboardWriteOutcome.DENIED,
            ClipboardWriteOutcome.FAILED,
        )) {
            val vm = clipboard116Model(ShareLinkClipboard { outcome })
            vm.copyLink(clipboard116Share())
            assertNull(vm.clipboardLink)
            assertFalse(vm.isCopyingLink)
            assertTrue(vm.copyError != null)
            assertFalse(vm.copyError!!.contains("CaseToken"))
            vm.clearCopyError()
            assertNull(vm.copyError)
        }
    }

    @Test fun api116UnexpectedOutputFailureHasSafeCopyFeedback() = clipboard116Test {
        val vm = clipboard116Model(ShareLinkClipboard { error("private clipboard exception") })
        vm.copyLink(clipboard116Share())
        assertEquals("The link could not be copied. Try again.", vm.copyError)
        assertNull(vm.clipboardLink)
        assertFalse(vm.isCopyingLink)
    }

    @Test fun api116RepeatedClicksCannotStartCompetingWrites() = clipboard116Test {
        val ack = CompletableDeferred<ClipboardWriteOutcome>()
        var calls = 0
        val vm = clipboard116Model(
            ShareLinkClipboard {
                calls++
                ack.await()
            },
        )
        vm.copyLink(clipboard116Share("first"))
        vm.copyLink(clipboard116Share("second"))
        assertEquals(1, calls)
        ack.complete(ClipboardWriteOutcome.COPIED)
        runCurrent()
        assertEquals("https://viewer.invalid/gateway/api/v1/share/first/view", vm.clipboardLink)
    }

    @Test fun api116DeadlineClearsProgressWithoutInventingSuccessAndRetryWorks() = clipboard116Test {
        var next: ClipboardWriteOutcome? = null
        val vm = clipboard116Model(ShareLinkClipboard { next ?: CompletableDeferred<ClipboardWriteOutcome>().await() })
        vm.copyLink(clipboard116Share())
        advanceTimeBy(5001)
        runCurrent()
        assertFalse(vm.isCopyingLink)
        assertNull(vm.clipboardLink)
        assertEquals("The clipboard did not respond. Try again.", vm.copyError)
        next = ClipboardWriteOutcome.COPIED
        vm.copyLink(clipboard116Share())
        assertTrue(vm.clipboardLink != null)
        assertNull(vm.copyError)
    }

    @Test fun api116CancellationDoesNotBecomeAnErrorOrSuccess() = clipboard116Test {
        val vm = clipboard116Model(ShareLinkClipboard { throw CancellationException("cancel") })
        vm.copyLink(clipboard116Share())
        assertFalse(vm.isCopyingLink)
        assertNull(vm.clipboardLink)
        assertNull(vm.copyError)
    }

    @Test fun api116LeavingTheScreenSuppressesLateSuccessWithoutOverwritingANewerRequest() = clipboard116Test {
        val oldAck = CompletableDeferred<ClipboardWriteOutcome>()
        var calls = 0
        val vm = clipboard116Model(
            ShareLinkClipboard {
                if (calls++ == 0) withContext(NonCancellable) { oldAck.await() } else ClipboardWriteOutcome.COPIED
            },
        )
        vm.copyLink(clipboard116Share("old"))
        vm.cancelCopy()
        assertFalse(vm.isCopyingLink)
        assertNull(vm.clipboardLink)
        assertNull(vm.copyError)
        vm.copyLink(clipboard116Share("new"))
        oldAck.complete(ClipboardWriteOutcome.COPIED)
        runCurrent()
        assertEquals("https://viewer.invalid/gateway/api/v1/share/new/view", vm.clipboardLink)
        assertFalse(vm.isCopyingLink)
    }

    @Test fun api116ARefusedWriteCanBeRetriedAfterActivation() = clipboard116Test {
        var outcome = ClipboardWriteOutcome.DENIED
        val vm = clipboard116Model(ShareLinkClipboard { outcome })
        vm.copyLink(clipboard116Share())
        assertNull(vm.clipboardLink)
        outcome = ClipboardWriteOutcome.COPIED
        vm.copyLink(clipboard116Share())
        assertTrue(vm.clipboardLink != null)
        assertNull(vm.copyError)
    }
}
