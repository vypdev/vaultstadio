/**
 * Event subscription descriptor.
 */

package com.vaultstadio.domain.common.event

/**
 * Event subscription.
 */
data class EventSubscription(
    val id: String,
    val eventType: Class<out StorageEvent>,
    val handlerId: String,
)
