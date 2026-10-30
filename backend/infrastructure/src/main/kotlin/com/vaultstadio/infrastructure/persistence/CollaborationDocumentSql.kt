package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.DocumentState
import com.vaultstadio.infrastructure.persistence.entities.CollaborationOperationsTable
import com.vaultstadio.infrastructure.persistence.entities.DocumentStatesTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

internal fun insertCollaborationOperation(itemId: String, operation: CollaborationOperation) {
    CollaborationOperationsTable.insert {
        it[id] = operation.id
        it[CollaborationOperationsTable.itemId] = itemId
        it[userId] = operation.userId
        it[operationType] = when (operation) {
            is CollaborationOperation.Insert -> "INSERT"
            is CollaborationOperation.Delete -> "DELETE"
            is CollaborationOperation.Retain -> "RETAIN"
        }
        it[position] = when (operation) {
            is CollaborationOperation.Insert -> operation.position
            is CollaborationOperation.Delete -> operation.position
            is CollaborationOperation.Retain -> 0
        }
        it[text] = when (operation) {
            is CollaborationOperation.Insert -> operation.text
            else -> null
        }
        it[length] = when (operation) {
            is CollaborationOperation.Delete -> operation.length
            is CollaborationOperation.Retain -> operation.count
            else -> null
        }
        it[baseVersion] = operation.baseVersion
        it[timestamp] = operation.timestamp
    }
}

internal fun persistCollaborationDocument(state: DocumentState, exists: Boolean) {
    if (exists) {
        DocumentStatesTable.update({ DocumentStatesTable.itemId eq state.itemId }) {
            it[version] = state.version
            it[content] = state.content
            it[lastModified] = state.lastModified
        }
    } else {
        DocumentStatesTable.insert {
            it[id] = UUID.randomUUID().toString()
            it[itemId] = state.itemId
            it[version] = state.version
            it[content] = state.content
            it[lastModified] = state.lastModified
        }
    }
}
