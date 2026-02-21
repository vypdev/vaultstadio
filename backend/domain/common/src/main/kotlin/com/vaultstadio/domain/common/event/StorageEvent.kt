/**
 * Base type for all storage/domain events.
 */

package com.vaultstadio.domain.common.event

import kotlinx.datetime.Instant

/**
 * Base interface for all storage events.
 * Concrete event types (e.g. FileEvent, UserEvent) are defined in their respective domain modules.
 */
interface StorageEvent {
    val id: String
    val timestamp: Instant
    val userId: String?
}
