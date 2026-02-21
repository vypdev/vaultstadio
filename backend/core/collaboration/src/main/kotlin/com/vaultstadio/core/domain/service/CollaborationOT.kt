/**
 * Operational Transformation helpers for collaboration.
 * Extracted from CollaborationService to keep the main file under the line limit.
 */

package com.vaultstadio.core.domain.service

import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.DocumentState

/**
 * Transform an operation for OT conflict resolution.
 */
internal fun transformCollaborationOperation(
    operation: CollaborationOperation,
    state: DocumentState,
): CollaborationOperation {
    if (operation.baseVersion < state.version) {
        val concurrentOps = state.operations.filter {
            it.timestamp > operation.timestamp
        }
        return concurrentOps.fold(operation) { op, concurrent ->
            transformCollaborationAgainst(op, concurrent)
        }
    }
    return operation
}

/**
 * Transform an operation against a concurrent operation.
 */
internal fun transformCollaborationAgainst(
    op: CollaborationOperation,
    concurrent: CollaborationOperation,
): CollaborationOperation {
    return when (op) {
        is CollaborationOperation.Insert -> {
            when (concurrent) {
                is CollaborationOperation.Insert -> {
                    if (concurrent.position <= op.position) {
                        op.copy(position = op.position + concurrent.text.length)
                    } else {
                        op
                    }
                }
                is CollaborationOperation.Delete -> {
                    if (concurrent.position < op.position) {
                        op.copy(position = maxOf(concurrent.position, op.position - concurrent.length))
                    } else {
                        op
                    }
                }
                is CollaborationOperation.Retain -> op
            }
        }
        is CollaborationOperation.Delete -> {
            when (concurrent) {
                is CollaborationOperation.Insert -> {
                    if (concurrent.position <= op.position) {
                        op.copy(position = op.position + concurrent.text.length)
                    } else if (concurrent.position < op.position + op.length) {
                        op.copy(length = op.length + concurrent.text.length)
                    } else {
                        op
                    }
                }
                is CollaborationOperation.Delete -> {
                    if (concurrent.position >= op.position + op.length) {
                        op
                    } else if (concurrent.position + concurrent.length <= op.position) {
                        op.copy(position = op.position - concurrent.length)
                    } else {
                        op
                    }
                }
                is CollaborationOperation.Retain -> op
            }
        }
        is CollaborationOperation.Retain -> op
    }
}
