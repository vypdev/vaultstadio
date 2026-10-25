/**
 * Type of sync change.
 */

package com.vaultstadio.app.domain.sync.model

enum class ChangeType {
    CREATE,
    MODIFY,
    RENAME,
    MOVE,
    DELETE,
    RESTORE,
    TRASH,
    METADATA,
}
