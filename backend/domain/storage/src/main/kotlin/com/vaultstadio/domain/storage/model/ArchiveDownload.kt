package com.vaultstadio.domain.storage.model

/** Fixed upper bounds; tests may select stricter budgets, never larger production limits. */
data class ArchiveLimits(
    val entries: Int = 256,
    val depth: Int = 16,
    val inputBytes: Long = 24L * 1024 * 1024,
    val outputBytes: Int = 32 * 1024 * 1024,
) {
    init {
        require(entries in 1..256 && depth in 1..16)
        require(inputBytes in 1..(24L * 1024 * 1024) && outputBytes in 1..(32 * 1024 * 1024))
    }
}

data class ArchiveEntry(val path: String, val item: StorageItem)

fun isSafeArchiveSegment(name: String): Boolean =
    name.isNotBlank() &&
        name.length <= 255 &&
        name != "." &&
        name != ".." &&
        name.none { it == '/' || it == '\\' || it.code < 32 || it.code == 127 }
