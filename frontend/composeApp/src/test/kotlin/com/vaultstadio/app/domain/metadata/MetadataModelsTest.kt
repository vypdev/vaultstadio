/**
 * Unit tests for metadata domain models: MetadataSearchResult.
 */

package com.vaultstadio.app.domain.metadata

import com.vaultstadio.app.domain.metadata.model.MetadataSearchResult
import kotlin.test.Test
import kotlin.test.assertEquals

class MetadataSearchResultTest {

    @Test
    fun metadataSearchResult_construction() {
        val r = MetadataSearchResult(
            itemId = "item-1",
            itemName = "doc.pdf",
            itemPath = "/docs/doc.pdf",
            pluginId = "image-metadata",
            key = "camera",
            value = "Canon EOS",
        )
        assertEquals("item-1", r.itemId)
        assertEquals("doc.pdf", r.itemName)
        assertEquals("/docs/doc.pdf", r.itemPath)
        assertEquals("image-metadata", r.pluginId)
        assertEquals("camera", r.key)
        assertEquals("Canon EOS", r.value)
    }

    @Test
    fun metadataSearchResult_withEmptyValue() {
        val r = MetadataSearchResult(
            itemId = "i2",
            itemName = "photo.jpg",
            itemPath = "/photos/photo.jpg",
            pluginId = "video-metadata",
            key = "duration",
            value = "",
        )
        assertEquals("i2", r.itemId)
        assertEquals("", r.value)
    }
}
