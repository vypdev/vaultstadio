package com.vaultstadio.domain.storage.model

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StorageSelectionTest {
    @ParameterizedTest(name = "P91-policy-invalid {0}/{1}/{2}")
    @CsvSource(
        "-1,0,LIMIT",
        "0,0,LIMIT",
        "1001,0,LIMIT",
        "2147483647,0,LIMIT",
        "-2147483648,0,LIMIT",
        "1,-1,OFFSET",
        "100,-2147483648,OFFSET",
        "0,-1,LIMIT",
    )
    fun invalidSelectionHasSemanticParameter(limit: Int, offset: Int, parameter: SelectionParameter) {
        val result = StorageSelection.create(limit, offset)
        assertTrue(result.isLeft())
        assertEquals(parameter, result.leftOrNull()!!.parameter)
    }

    @ParameterizedTest(name = "P91-policy-valid {0}/{1}")
    @CsvSource("1,0", "100,0", "1000,0", "1,2147483647", "1000,2147483647", "20,1")
    fun validBoundariesRetainExactValues(limit: Int, offset: Int) {
        val result = StorageSelection.create(limit, offset)
        assertTrue(result.isRight())
        assertEquals(limit, result.getOrNull()!!.limit)
        assertEquals(offset, result.getOrNull()!!.offset)
    }
}
