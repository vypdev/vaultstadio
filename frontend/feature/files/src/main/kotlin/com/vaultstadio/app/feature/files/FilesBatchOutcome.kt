package com.vaultstadio.app.feature.files

import com.vaultstadio.app.domain.storage.model.BatchResult

internal data class FilesBatchOutcome(val remaining: Set<String>, val error: String?)

/** A successful HTTP envelope can still contain failed or uncertain per-item effects. */
internal fun BatchResult.forSelection(requested: Set<String>): FilesBatchOutcome {
    if (failed == 0 && successful == requested.size && errors.isEmpty()) return FilesBatchOutcome(emptySet(), null)
    val failedIds = errors.map { it.itemId }.toSet()
    val completeFailures = failed > 0 &&
        successful >= 0 &&
        successful.toLong() + failed.toLong() == requested.size.toLong() &&
        failedIds.size == failed &&
        errors.size == failed &&
        failedIds.all { it in requested }
    val retained = if (completeFailures) failedIds else requested
    val details = errors.map { it.error }.filter { it.isNotBlank() }.distinct().joinToString("; ")
    val message = if (completeFailures) {
        details.ifBlank { "$failed items could not be processed" }
    } else {
        "Some results could not be confirmed. Inspect the selection before retrying."
    }
    return FilesBatchOutcome(retained, message)
}
