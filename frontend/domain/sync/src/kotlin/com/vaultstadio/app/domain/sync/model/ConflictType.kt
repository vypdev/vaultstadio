/**
 * Type of sync conflict.
 */

package com.vaultstadio.app.domain.sync.model

enum class ConflictType {
    EDIT_CONFLICT,
    EDIT_DELETE,
    DELETE_EDIT,
    CREATE_CREATE,
    MOVE_MOVE,
    PARENT_DELETED,
}
