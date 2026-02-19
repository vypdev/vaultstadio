/**
 * VaultStadio Event Bus Tests
 */

package com.vaultstadio.core.domain.event

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EventBusTest {

    private lateinit var eventBus: EventBus

    @BeforeEach
    fun setup() {
        eventBus = EventBus()
    }

    @AfterEach
    fun teardown() {
        eventBus.shutdown()
    }

    // Test event for use in tests
    private fun createTestEvent(): FileEvent.Uploaded {
        val testItem = com.vaultstadio.core.domain.model.StorageItem(
            id = "item-123",
            name = "test.txt",
            path = "/test.txt",
            type = com.vaultstadio.core.domain.model.ItemType.FILE,
            ownerId = "user-123",
            size = 1024,
            mimeType = "text/plain",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )
        return FileEvent.Uploaded(
            id = "test-event-${System.currentTimeMillis()}",
            timestamp = Clock.System.now(),
            userId = "user-123",
            item = testItem,
        )
    }

    @Nested
    inner class SubscribeTests {

        @Test
        fun `subscribe should register handler`() {
            val handler = EventHandler<FileEvent.Uploaded> {
                EventHandlerResult.Success
            }

            eventBus.subscribe(FileEvent.Uploaded::class.java, "test-handler", handler)

            assertEquals(1, eventBus.handlerCount())
        }

        @Test
        fun `subscribe should allow multiple handlers`() {
            val handler1 = EventHandler<FileEvent.Uploaded> { EventHandlerResult.Success }
            val handler2 = EventHandler<FileEvent.Uploaded> { EventHandlerResult.Success }

            eventBus.subscribe(FileEvent.Uploaded::class.java, "handler-1", handler1)
            eventBus.subscribe(FileEvent.Uploaded::class.java, "handler-2", handler2)

            assertEquals(2, eventBus.handlerCount())
        }

        @Test
        fun `subscribe should return subscription`() {
            val handler = EventHandler<FileEvent.Uploaded> { EventHandlerResult.Success }

            val subscription = eventBus.subscribe(
                FileEvent.Uploaded::class.java,
                "test-handler",
                handler,
            )

            assertEquals("test-handler:Uploaded", subscription.id)
            assertEquals("test-handler", subscription.handlerId)
        }
    }

    @Nested
    inner class UnsubscribeTests {

        @Test
        fun `unsubscribe should remove handler`() {
            val handler = EventHandler<FileEvent.Uploaded> { EventHandlerResult.Success }

            val subscription = eventBus.subscribe(
                FileEvent.Uploaded::class.java,
                "test-handler",
                handler,
            )

            eventBus.unsubscribe(subscription)

            assertEquals(0, eventBus.handlerCount())
        }

        @Test
        fun `unsubscribeAll should remove all handlers for id`() {
            val handler1 = EventHandler<FileEvent.Uploaded> { EventHandlerResult.Success }
            val handler2 = EventHandler<FileEvent.Deleted> { EventHandlerResult.Success }

            eventBus.subscribe(FileEvent.Uploaded::class.java, "plugin-1", handler1)
            eventBus.subscribe(FileEvent.Deleted::class.java, "plugin-1", handler2)

            eventBus.unsubscribeAll("plugin-1")

            assertEquals(0, eventBus.handlerCount())
        }
    }

    @Nested
    inner class PublishTests {

        @Test
        fun `publish should call registered handlers`() = runTest {
            val callCount = AtomicInteger(0)

            val handler = EventHandler<FileEvent.Uploaded> {
                callCount.incrementAndGet()
                EventHandlerResult.Success
            }

            eventBus.subscribe(FileEvent.Uploaded::class.java, "test-handler", handler)

            eventBus.publish(createTestEvent(), async = false)

            assertEquals(1, callCount.get())
        }

        @Test
        fun `publish should call multiple handlers`() = runTest {
            val callCount = AtomicInteger(0)

            val handler1 = EventHandler<FileEvent.Uploaded> {
                callCount.incrementAndGet()
                EventHandlerResult.Success
            }
            val handler2 = EventHandler<FileEvent.Uploaded> {
                callCount.incrementAndGet()
                EventHandlerResult.Success
            }

            eventBus.subscribe(FileEvent.Uploaded::class.java, "handler-1", handler1)
            eventBus.subscribe(FileEvent.Uploaded::class.java, "handler-2", handler2)

            eventBus.publish(createTestEvent(), async = false)

            assertEquals(2, callCount.get())
        }

        @Test
        fun `publish should not fail when no handlers registered`() = runTest {
            // Should not throw
            eventBus.publish(createTestEvent())
        }

        @Test
        fun `publish async should not block`() = runTest {
            val handler = EventHandler<FileEvent.Uploaded> {
                delay(1000) // Slow handler
                EventHandlerResult.Success
            }

            eventBus.subscribe(FileEvent.Uploaded::class.java, "slow-handler", handler)

            val start = System.currentTimeMillis()
            eventBus.publish(createTestEvent(), async = true)
            val elapsed = System.currentTimeMillis() - start

            // Should return quickly without waiting for handler
            assertTrue(elapsed < 500)
        }
    }

    @Nested
    inner class EventFlowTests {

        @Test
        fun `eventFlow should have subscribers`() = runTest {
            // Verify event flow is accessible
            assertNotNull(eventBus.eventFlow)
        }

        @Test
        fun `eventsOfType should return filtered flow`() = runTest {
            // Verify typed flow is accessible
            val flow = eventBus.eventsOfType<FileEvent.Uploaded>()
            assertNotNull(flow)
        }
    }

    @Nested
    inner class ErrorHandlingTests {

        @Test
        fun `handler error should not affect other handlers`() = runTest {
            val successCount = AtomicInteger(0)

            val failingHandler = EventHandler<FileEvent.Uploaded> {
                throw RuntimeException("Handler failed")
            }

            val successHandler = EventHandler<FileEvent.Uploaded> {
                successCount.incrementAndGet()
                EventHandlerResult.Success
            }

            eventBus.subscribe(FileEvent.Uploaded::class.java, "failing", failingHandler)
            eventBus.subscribe(FileEvent.Uploaded::class.java, "success", successHandler)

            eventBus.publish(createTestEvent(), async = false)

            assertEquals(1, successCount.get())
        }

        @Test
        fun `handler returning error should be logged`() = runTest {
            val handler = EventHandler<FileEvent.Uploaded> {
                EventHandlerResult.Error(RuntimeException("Something went wrong"))
            }

            eventBus.subscribe(FileEvent.Uploaded::class.java, "error-handler", handler)

            // Should not throw, just log error
            eventBus.publish(createTestEvent(), async = false)
        }
    }

    @Nested
    inner class ShutdownTests {

        @Test
        fun `shutdown should clear all handlers`() {
            val handler = EventHandler<FileEvent.Uploaded> { EventHandlerResult.Success }

            eventBus.subscribe(FileEvent.Uploaded::class.java, "test", handler)
            assertEquals(1, eventBus.handlerCount())

            eventBus.shutdown()

            assertEquals(0, eventBus.handlerCount())
        }
    }
}
