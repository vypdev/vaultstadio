/**
 * Helper to apply a collaboration operation to document content.
 * Extracted from ExposedCollaborationRepository to keep the main file under the line limit.
 */

package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.core.domain.model.CollaborationOperation

/**
 * Apply an operation to document content (insert/delete/retain).
 */
internal fun applyOperationToContent(content: String, operation: CollaborationOperation): String {
    return when (operation) {
        is CollaborationOperation.Insert -> {
            val pos = operation.position.coerceIn(0, content.length)
            content.substring(0, pos) + operation.text + content.substring(pos)
        }
        is CollaborationOperation.Delete -> {
            val start = operation.position.coerceIn(0, content.length)
            val end = (start + operation.length).coerceIn(0, content.length)
            content.substring(0, start) + content.substring(end)
        }
        is CollaborationOperation.Retain -> content
    }
}
