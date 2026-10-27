package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.core.domain.model.PresenceStatus
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.DatabaseException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class CollaborationStreamLifecycleTest {
    @Test
    fun `operation stream checks ownership again before every delivery`() = runTest {
        val f = CollaborationServiceFixture()
        every { f.repository.streamOperations(f.item.id) } returns flow {
            emit(f.operation)
            f.item = f.item.copy(ownerId = "revoked")
            emit(f.operation)
        }
        val source = f.service.streamOperations(f.item.id, "user-1").requiredStream()
        val received = mutableListOf<CollaborationOperation>()
        assertFailsWith<AuthorizationException> { source.collect { received.add(it) } }
        assertEquals(listOf<CollaborationOperation>(f.operation), received.toList())
    }

    @Test
    fun `comment stream validates each returned parent before exposing it`() = runTest {
        val f = CollaborationServiceFixture()
        every { f.repository.streamComments(f.item.id) } returns flowOf(f.comment.copy(itemId = "foreign"))
        val source = f.service.streamComments(f.item.id, "user-1").requiredStream()
        val received = mutableListOf<DocumentComment>()
        assertFailsWith<DatabaseException> { source.collect { received.add(it) } }
        assertEquals(emptyList(), received)
    }

    @Test
    fun `comment stream rechecks ownership before collection and does not touch provider when revoked`() = runTest {
        val f = CollaborationServiceFixture()
        val source = f.service.streamComments(f.item.id, "user-1").requiredStream()
        f.item = f.item.copy(ownerId = "revoked")
        assertFailsWith<AuthorizationException> { source.toList() }
        verify(exactly = 0) { f.repository.streamComments(any()) }
    }

    @Test
    fun `empty presence subscription cannot expose even self and does not initialize provider`() = runTest {
        val f = CollaborationServiceFixture()
        val source = f.service.streamPresence(emptyList(), "user-1").requiredStream()
        assertEquals(emptyList(), source.toList())
        verify(exactly = 0) { f.repository.streamPresence(any()) }
    }

    @Test
    fun `operation provider cancellation retains exact control flow`() = runTest {
        val f = CollaborationServiceFixture()
        val cancel = CancellationException("Deliberate stream cancellation")
        every { f.repository.streamOperations(any()) } returns flow { throw cancel }
        val source = f.service.streamOperations(f.item.id, "user-1").requiredStream()
        assertSame(cancel, assertFailsWith<CancellationException> { source.toList() })
    }

    @Test
    fun `metadata checks and comment delivery may suspend without losing actor scope`() = runTest {
        val f = CollaborationServiceFixture()
        coEvery { f.items.findById(any()) } coAnswers {
            yield()
            f.item.right()
        }
        every { f.repository.streamComments(any()) } returns flow {
            yield()
            emit(f.comment)
        }
        val source = f.service.streamComments(f.item.id, "user-1").requiredStream()
        assertEquals(listOf(f.comment), source.toList())
    }

    @Test
    fun `presence may bind an owned active session without an optional document pointer`() = runTest {
        val f = CollaborationServiceFixture()
        val presence = f.service.updatePresence("user-1", PresenceStatus.ONLINE, activeSession = f.session.id)
            .requiredStream()
        assertEquals(f.session.id, presence.activeSession)
        assertEquals(null, presence.activeDocument)
    }

    @Test
    fun `typed comment provider failure remains the original failure`() = runTest {
        val f = CollaborationServiceFixture()
        val failure = DatabaseException("Private diagnostic remains internal")
        every { f.repository.streamComments(any()) } returns flow { throw failure }
        val source = f.service.streamComments(f.item.id, "user-1").requiredStream()
        assertSame(failure, assertFailsWith<DatabaseException> { source.toList() })
    }
}

private fun <A : Throwable, B> Either<A, B>.requiredStream(): B = fold({ throw it }, { it })
