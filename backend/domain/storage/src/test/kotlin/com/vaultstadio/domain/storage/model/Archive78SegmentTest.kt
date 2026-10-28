package com.vaultstadio.domain.storage.model

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
}
