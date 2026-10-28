package com.vaultstadio.domain.storage.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Archive78SegmentTest {
    @ParameterizedTest
    @ValueSource(strings = ["C:", "c:payload.bin", "D:folder", ":prefix", "file:stream", "C:\\escape", "C:/escape", "safe:C"])
    fun `portable ZIP segments reject drive prefixes and colon syntax`(name: String) {
        assertFalse(isSafeArchiveSegment(name))
    }

    @ParameterizedTest
    @ValueSource(strings = ["C", "file.bin", "música ñ.txt", "日本語", ".hidden", "😀"])
    fun `portable ZIP segments retain ordinary Unicode names`(name: String) {
        assertTrue(isSafeArchiveSegment(name))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", ".", "..", "safe/file", "safe\\file"])
    fun `blank navigation and separator segments cannot become ZIP entries`(name: String) {
        assertFalse(isSafeArchiveSegment(name))
    }

    @Test fun `forbidden control characters cannot hide inside a ZIP segment`() {
        for (character in listOf('\u0000', '\u001f', '\u007f')) {
            assertFalse(isSafeArchiveSegment("before${character}after"))
        }
    }

    @Test fun `exact maximum supported segment length is retained`() {
        assertTrue(isSafeArchiveSegment("a".repeat(255)))
    }

    @Test fun `first unsupported segment length is rejected`() {
        assertFalse(isSafeArchiveSegment("a".repeat(256)))
    }

}
