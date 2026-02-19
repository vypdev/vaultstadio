/**
 * Folder file for upload: name, relative path in the folder tree, and content.
 */

package com.vaultstadio.app.domain.upload

data class FolderUploadEntry(
    val name: String,
    val relativePath: String,
    val size: Long,
    val mimeType: String,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as FolderUploadEntry
        return name == other.name && relativePath == other.relativePath && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + relativePath.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
