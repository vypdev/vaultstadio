/**
 * VaultStadio Event Bus
 *
 * In-memory event bus for publishing and subscribing to events.
 * Supports both synchronous and asynchronous event handling.
 */

package com.vaultstadio.core.domain.event

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Event handler result.
 */
sealed class EventHandlerResult {
    data object Success : EventHandlerResult()
    data class Error(val exception: Throwable) : EventHandlerResult()
}

/**
 * Event handler interface.
 */
fun interface EventHandler<T : StorageEvent> {
    suspend fun handle(event: T): EventHandlerResult
}

/**
 * Event subscription.
 */
data class EventSubscription(
    val id: String,
    val eventType: Class<out StorageEvent>,
    val handlerId: String,
) {
    fun unsubscribe(eventBus: EventBus) {
        eventBus.unsubscribe(this)
    }
}

/**
 * Event bus for publishing and subscribing to storage events.
 *
 * This is the central hub for event-driven communication between
 * the core storage engine and plugins.
 */
class EventBus(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) {
    private val handlers = ConcurrentHashMap<Class<out StorageEvent>, MutableList<Pair<String, EventHandler<*>>>>()
    private val _eventFlow = MutableSharedFlow<StorageEvent>(extraBufferCapacity = 1000)

    /**
     * Flow of all events for reactive subscribers.
     */
    val eventFlow: SharedFlow<StorageEvent> = _eventFlow.asSharedFlow()

    /**
     * Publishes an event to all subscribers.
     *
     * @param event The event to publish
     * @param async If true, handlers are called asynchronously
     */
    suspend fun <T : StorageEvent> publish(event: T, async: Boolean = true) {
        logger.debug { "Publishing event: ${event::class.simpleName} (id: ${event.id})" }

        // Emit to flow for reactive subscribers
        _eventFlow.emit(event)

        // Get handlers for this event type and all parent types
        val applicableHandlers = mutableListOf<Pair<String, EventHandler<*>>>()

        var currentClass: Class<*>? = event::class.java
        while (currentClass != null && StorageEvent::class.java.isAssignableFrom(currentClass)) {
            @Suppress("UNCHECKED_CAST")
            handlers[currentClass as Class<out StorageEvent>]?.let {
                applicableHandlers.addAll(it)
            }
            currentClass = currentClass.superclass
        }

        if (applicableHandlers.isEmpty()) {
            logger.trace { "No handlers registered for event type: ${event::class.simpleName}" }
            return
        }

        if (async) {
            // Fire and forget - handlers run in parallel
            applicableHandlers.forEach { (handlerId, handler) ->
                scope.launch {
                    executeHandler(handlerId, handler, event)
                }
            }
        } else {
            // Synchronous - handlers run sequentially
            applicableHandlers.forEach { (handlerId, handler) ->
                executeHandler(handlerId, handler, event)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : StorageEvent> executeHandler(
        handlerId: String,
        handler: EventHandler<*>,
        event: T,
    ) {
        try {
            val typedHandler = handler as EventHandler<T>
            when (val result = typedHandler.handle(event)) {
                is EventHandlerResult.Success -> {
                    logger.trace { "Handler $handlerId successfully processed event ${event.id}" }
                }
                is EventHandlerResult.Error -> {
                    logger.error(result.exception) {
                        "Handler $handlerId failed to process event ${event.id}"
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Handler $handlerId threw exception for event ${event.id}" }
        }
    }

    /**
     * Subscribes to events of a specific type.
     *
     * @param eventType The event type to subscribe to
     * @param handlerId Unique identifier for this handler
     * @param handler The event handler
     * @return Subscription that can be used to unsubscribe
     */
    fun <T : StorageEvent> subscribe(
        eventType: Class<T>,
        handlerId: String,
        handler: EventHandler<T>,
    ): EventSubscription {
        handlers.computeIfAbsent(eventType) { mutableListOf() }
            .add(handlerId to handler)

        logger.info { "Handler $handlerId subscribed to ${eventType.simpleName}" }

        return EventSubscription(
            id = "$handlerId:${eventType.simpleName}",
            eventType = eventType,
            handlerId = handlerId,
        )
    }

    /**
     * Subscribes to events using reified type parameter.
     */
    inline fun <reified T : StorageEvent> subscribe(
        handlerId: String,
        handler: EventHandler<T>,
    ): EventSubscription = subscribe(T::class.java, handlerId, handler)

    /**
     * Unsubscribes from events.
     *
     * @param subscription The subscription to remove
     */
    fun unsubscribe(subscription: EventSubscription) {
        handlers[subscription.eventType]?.removeIf { (id, _) -> id == subscription.handlerId }
        logger.info { "Handler ${subscription.handlerId} unsubscribed from ${subscription.eventType.simpleName}" }
    }

    /**
     * Unsubscribes all handlers for a specific handler ID.
     *
     * @param handlerId The handler ID to remove
     */
    fun unsubscribeAll(handlerId: String) {
        handlers.values.forEach { list ->
            list.removeIf { (id, _) -> id == handlerId }
        }
        logger.info { "All handlers for $handlerId unsubscribed" }
    }

    /**
     * Returns a flow filtered by event type.
     */
    inline fun <reified T : StorageEvent> eventsOfType(): Flow<T> =
        eventFlow.filterIsInstance()

    /**
     * Gets the count of registered handlers.
     */
    fun handlerCount(): Int = handlers.values.sumOf { it.size }

    /**
     * Shuts down the event bus.
     */
    fun shutdown() {
        scope.cancel()
        handlers.clear()
        logger.info { "Event bus shut down" }
    }
}
