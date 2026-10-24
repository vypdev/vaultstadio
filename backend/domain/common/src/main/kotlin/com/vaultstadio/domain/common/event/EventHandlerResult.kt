/**
 * Result of an event handler execution.
 */

package com.vaultstadio.domain.common.event

/**
 * Event handler result.
 */
sealed class EventHandlerResult {
    data object Success : EventHandlerResult()
    data class Error(val exception: Throwable) : EventHandlerResult()
}
