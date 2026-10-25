/**
 * Event bus port for publishing and subscribing to domain events.
 * Implementations live in core or infrastructure.
 */

package com.vaultstadio.domain.common.event

/**
 * Event bus for publishing and subscribing to storage/domain events.
 * This is the port; the implementation is provided by the core layer.
 */
interface EventBus {

    /**
     * Publishes an event to all subscribers.
     *
     * @param event The event to publish
     * @param async If true, handlers are called asynchronously
     */
    suspend fun <T : StorageEvent> publish(event: T, async: Boolean = true)

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
    ): EventSubscription

    /**
     * Unsubscribes from events.
     *
     * @param subscription The subscription to remove
     */
    fun unsubscribe(subscription: EventSubscription)

    /**
     * Unsubscribes all handlers for a specific handler ID.
     *
     * @param handlerId The handler ID to remove
     */
    fun unsubscribeAll(handlerId: String)
}
