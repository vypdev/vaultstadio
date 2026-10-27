package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.core.domain.model.DocumentState
import com.vaultstadio.domain.common.exception.InvalidOperationException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.infrastructure.persistence.entities.DocumentStatesTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

/** Call only inside the item-owner transaction, preserving item -> document lock order. */
internal fun requireCollaborationSnapshot(expected: DocumentState): DocumentState {
    val current = DocumentStatesTable.selectAll().where { DocumentStatesTable.itemId eq expected.itemId }
        .forUpdate().singleOrNull()?.toDocumentState()
        ?: throw ItemNotFoundException(message = "Document state not found")
    if (current.version != expected.version ||
        current.content != expected.content ||
        current.lastModified != expected.lastModified
    ) {
        throw InvalidOperationException("save_snapshot", "Document changed before write; read state and retry")
    }
    return current
}
