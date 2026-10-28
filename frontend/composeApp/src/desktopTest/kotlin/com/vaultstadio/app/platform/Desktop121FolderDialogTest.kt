package com.vaultstadio.app.platform

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.awt.EventQueue
import java.awt.Toolkit
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Actual event queue and modal secondary loop; no display-server or fabricated EDT state. */
class Desktop121FolderDialogTest {
    @Test fun folder121DesktopOwnsConstructionSelectionAndDisposalOnEdt() = runBlocking {
        val directory = File("owned niñó 日本語")
        val closed = AtomicBoolean()
        val selected = withTimeout(5000) {
            chooseDesktopFolder {
                assertTrue(EventQueue.isDispatchThread(), "factory must own EDT")
                dialog(
                    select = {
                        assertTrue(EventQueue.isDispatchThread())
                        directory
                    },
                    close = {
                        assertTrue(EventQueue.isDispatchThread())
                        closed.set(true)
                    },
                )
            }
        }
        assertEquals(directory, selected)
        assertTrue(closed.get())
    }

    @Test fun folder121DesktopNativeCancelDoesNotRead() = runBlocking {
        val closed = AtomicBoolean()
        val selected = withTimeout(5000) {
            selectDesktopFolderFiles(
                choose = { chooseDesktopFolder { dialog(select = { null }, close = { closed.set(true) }) } },
                read = { error("canceled selection must not enumerate") },
            )
        }
        assertTrue(selected.isEmpty())
        assertTrue(closed.get())
    }

    @Test fun folder121DesktopFactoryFailureReachesCaller() = runBlocking {
        val failure = SecurityException("owned folder factory failure")
        val observed = assertFailsWith<SecurityException> {
            withTimeout(5000) { chooseDesktopFolder { throw failure } }
        }
        assertEquals(failure.message, observed.message)
    }

    @Test fun folder121DesktopSelectionFailureDisposesAndReachesCaller() = runBlocking {
        val closed = AtomicBoolean()
        val failure = IllegalArgumentException("owned folder selection failure")
        val observed = assertFailsWith<IllegalArgumentException> {
            withTimeout(5000) {
                chooseDesktopFolder { dialog(select = { throw failure }, close = { closed.set(true) }) }
            }
        }
        assertEquals(failure.message, observed.message)
        assertTrue(closed.get())
    }

    @Test fun folder121DesktopDisposalFailureCannotClaimSelection() = runBlocking {
        val failure = SecurityException("owned folder disposal failure")
        val observed = assertFailsWith<SecurityException> {
            withTimeout(5000) {
                chooseDesktopFolder { dialog(select = { File("owned") }, close = { throw failure }) }
            }
        }
        assertEquals(failure.message, observed.message)
    }

    @Test fun folder121DesktopCancellationBeforeDispatchPreventsConstruction() = runBlocking {
        val ready = CompletableDeferred<Unit>()
        val drained = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val created = AtomicBoolean()
        EventQueue.invokeLater {
            ready.complete(Unit)
            release.await(5, TimeUnit.SECONDS)
        }
        try {
            withTimeout(5000) { ready.await() }
            val selection = async(start = CoroutineStart.UNDISPATCHED) {
                chooseDesktopFolder {
                    created.set(true)
                    dialog(select = { null })
                }
            }
            selection.cancelAndJoin()
            release.countDown()
            EventQueue.invokeLater { drained.complete(Unit) }
            withTimeout(5000) { drained.await() }
            assertFalse(created.get())
        } finally {
            release.countDown()
        }
    }

    @Test fun folder121DesktopActiveModalCancellationDisposesAndRejectsLateResult() = runBlocking {
        val shown = CompletableDeferred<Unit>()
        val disposed = CompletableDeferred<Unit>()
        val modal = Toolkit.getDefaultToolkit().systemEventQueue.createSecondaryLoop()
        val delivered = AtomicBoolean()
        val selection = async {
            val chosen = chooseDesktopFolder {
                dialog(
                    select = {
                        shown.complete(Unit)
                        modal.enter()
                        File("late-owned-directory")
                    },
                    close = {
                        assertTrue(EventQueue.isDispatchThread())
                        modal.exit()
                        disposed.complete(Unit)
                    },
                )
            }
            delivered.set(true)
            chosen
        }
        try {
            withTimeout(5000) {
                shown.await()
                selection.cancelAndJoin()
                disposed.await()
            }
            assertTrue(selection.isCancelled)
            assertFalse(delivered.get())
        } finally {
            EventQueue.invokeLater { modal.exit() }
            withTimeout(5000) { selection.cancelAndJoin() }
        }
    }

    @Test fun folder121DesktopApprovedReadRunsOutsideEdt() = runBlocking {
        val directory = Files.createTempDirectory("folder121-owned-").toFile()
        val bytes = byteArrayOf(0, 42, -1, 7)
        val nested = File(directory, "nested 日本語").apply { mkdir() }
        File(nested, "binary.bin").writeBytes(bytes)
        File(directory, "empty.dat").writeBytes(byteArrayOf())
        try {
            val selected = withTimeout(5000) {
                selectDesktopFolderFiles(
                    choose = { chooseDesktopFolder { dialog(select = { directory }) } },
                    read = { chosen ->
                        assertFalse(EventQueue.isDispatchThread(), "filesystem work must release EDT")
                        assertEquals(directory, chosen)
                        readDesktopFolderFiles(chosen)
                    },
                )
            }
            assertEquals(setOf("nested 日本語/binary.bin", "empty.dat"), selected.map { it.relativePath }.toSet())
            assertContentEquals(bytes, selected.single { it.name == "binary.bin" }.data)
            assertEquals(4L, selected.single { it.name == "binary.bin" }.size)
            assertTrue(selected.single { it.name == "empty.dat" }.data.isEmpty())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun folder121DesktopCancellationDisposalFailureCannotEscapeEdt() = runBlocking {
        val shown = CompletableDeferred<Unit>()
        val disposed = CompletableDeferred<Unit>()
        val drained = CompletableDeferred<Unit>()
        val modal = Toolkit.getDefaultToolkit().systemEventQueue.createSecondaryLoop()
        val selection = async {
            chooseDesktopFolder {
                dialog(
                    select = {
                        shown.complete(Unit)
                        modal.enter()
                        File("late-owned-directory")
                    },
                    close = {
                        assertTrue(EventQueue.isDispatchThread())
                        modal.exit()
                        disposed.complete(Unit)
                        throw SecurityException("owned canceled disposal failure")
                    },
                )
            }
        }
        try {
            withTimeout(5000) {
                shown.await()
                selection.cancelAndJoin()
                disposed.await()
            }
            EventQueue.invokeLater { drained.complete(Unit) }
            withTimeout(5000) { drained.await() }
            assertTrue(selection.isCancelled)
        } finally {
            EventQueue.invokeLater { modal.exit() }
            withTimeout(5000) { selection.cancelAndJoin() }
        }
    }

    private fun dialog(select: () -> File?, close: () -> Unit = {}): DesktopFolderDialog =
        object : DesktopFolderDialog {
            override fun select(): File? = select()
            override fun close() = close()
        }
}
