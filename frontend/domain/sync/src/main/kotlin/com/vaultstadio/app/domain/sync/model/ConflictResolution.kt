/**
 * Resolution strategy for a sync conflict.
 */

package com.vaultstadio.app.domain.sync.model

enum class ConflictResolution {
    KEEP_LOCAL,
    KEEP_REMOTE,
    KEEP_BOTH,
    MERGE,
    MANUAL,
}
