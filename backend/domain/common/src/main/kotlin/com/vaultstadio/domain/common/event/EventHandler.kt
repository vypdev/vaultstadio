/**
 * Event handler interface for the event bus.
 */

package com.vaultstadio.domain.common.event

/**
 * Event handler interface.
 */
fun interface EventHandler<T : StorageEvent> {
    suspend fun handle(event: T): EventHandlerResult
}
