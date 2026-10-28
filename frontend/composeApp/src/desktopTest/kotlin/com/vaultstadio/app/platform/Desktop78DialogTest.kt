package com.vaultstadio.app.platform

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.awt.EventQueue
import java.awt.Toolkit
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Uses the actual event queue; injected dialogs keep unit tests independent of a display server. */
class Desktop78DialogTest {
    @Test fun transfer78UnexpectedDialogFactoryFailureCompletesTheSuspendedCaller() = runBlocking {
        val failure = NullPointerException("owned dialog factory failure")
        val observed = assertFailsWith<NullPointerException> {
            withTimeout(5000) { chooseDownloadTarget("file.bin") { throw failure } }
        }
        assertEquals(failure.message, observed.message)
    }

    @Test fun transfer78UnexpectedSelectionFailureClosesDialogAndCompletesTheCaller() = runBlocking {
        val closed = AtomicBoolean()
        val failure = NullPointerException("owned dialog selection failure")
        val observed = assertFailsWith<NullPointerException> {
            withTimeout(5000) {
                chooseDownloadTarget("file.bin") {
                    object : DesktopDownloadDialog {
                        override fun select(name: String): Path? = throw failure
                        override fun close() {
                            closed.set(true)
                        }
                    }
                }
            }
        }
        assertEquals(failure.message, observed.message)
        assertTrue(closed.get())
    }

    @Test fun transfer78CloseFailureCannotEscapeTheEventQueueOrClaimAChosenDestination() = runBlocking {
        val failure = SecurityException("owned dialog disposal failure")
        val observed = assertFailsWith<SecurityException> {
            withTimeout(5000) {
                chooseDownloadTarget("file.bin") {
                    object : DesktopDownloadDialog {
                        override fun select(name: String): Path = Path.of("owned-target")
                        override fun close(): Unit = throw failure
                    }
                }
            }
        }
        assertEquals(failure.message, observed.message)
    }

    @Test fun transfer78NativeDialogSelectionAndDisposalRunOnTheRealEventQueue() = runBlocking {
        val selected = withTimeout(5000) {
            chooseDownloadTarget("música.bin") {
                assertTrue(EventQueue.isDispatchThread())
                object : DesktopDownloadDialog {
                    override fun select(name: String): Path {
                        assertTrue(EventQueue.isDispatchThread())
                        assertEquals("música.bin", name)
                        return Path.of(name)
                    }
                    override fun close() {
                        assertTrue(EventQueue.isDispatchThread())
                    }
                }
            }
        }
        assertEquals(Path.of("música.bin"), selected)
    }

    @Test fun transfer78CancellationClosesAModalDialogAndDoesNotReturnItsLateSelection() = runBlocking {
        val shown = CompletableDeferred<Unit>()
        val disposed = CompletableDeferred<Unit>()
        val closed = AtomicBoolean()
        val selection = async {
            chooseDownloadTarget("file.bin") {
                val modal = Toolkit.getDefaultToolkit().systemEventQueue.createSecondaryLoop()
                object : DesktopDownloadDialog {
                    override fun select(name: String): Path {
                        shown.complete(Unit)
                        modal.enter()
                        return Path.of(name)
                    }
                    override fun close() {
                        assertTrue(EventQueue.isDispatchThread())
                        closed.set(true)
                        modal.exit()
                        disposed.complete(Unit)
                    }
                }
            }
        }
        try {
            withTimeout(5000) { shown.await() }
            selection.cancelAndJoin()
            withTimeout(5000) { disposed.await() }
            assertTrue(selection.isCancelled)
            assertTrue(closed.get())
        } finally {
            selection.cancelAndJoin()
        }
    }
}
