package com.vaultstadio.api.routes.storage

import com.vaultstadio.domain.common.exception.ValidationException
import com.vaultstadio.domain.common.pagination.SortOrder
import com.vaultstadio.domain.storage.repository.SortField
import io.ktor.http.Parameters
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

internal class StorageSelectionParametersTest {
    @ParameterizedTest(name = "P91-parser-limit {0}")
    @ValueSource(strings = ["", " ", "-1", "+1", "1.5", "1e2", "١", "2147483648", "0", "1001", "99999999999999999999"])
    fun malformedOrOutOfRangeLimitNeverDefaults(value: String) = denied("limit", value)

    @ParameterizedTest(name = "P91-parser-offset {0}")
    @ValueSource(strings = ["", " ", "-1", "+0", "1.5", "1e2", "١", "2147483648", "99999999999999999999"])
    fun malformedOrNegativeOffsetNeverDefaults(value: String) = denied("offset", value)

    @ParameterizedTest(name = "P91-parser-default {0}")
    @CsvSource("FOLDER,100", "SEARCH,50", "RECENT,20", "SUGGESTIONS,10", "METADATA,50")
    fun absentValuesAloneUseSurfaceDefaults(surface: SelectionSurface, expected: Int) {
        val parsed = parseStorageSelection(Parameters.Empty, surface)
        assertEquals(expected, parsed.page.limit)
        assertEquals(0, parsed.page.offset)
        assertEquals(SortField.NAME, parsed.sortField)
        assertEquals(SortOrder.ASC, parsed.sortOrder)
    }

    @ParameterizedTest(name = "P91-parser-exact {0}/{1}")
    @CsvSource("0001,0000", "1000,2147483647", "20,9")
    fun validDecimalRepresentationRetainsItsValue(limit: String, offset: String) {
        val parsed =
            parseStorageSelection(
                Parameters.build {
                    append("limit", limit)
                    append("offset", offset)
                },
                SelectionSurface.FOLDER,
            )
        assertEquals(limit.toInt(), parsed.page.limit)
        assertEquals(offset.toInt(), parsed.page.offset)
    }

    @ParameterizedTest(name = "P91-parser-sort {0}/{1}")
    @CsvSource(
        "name,asc", "NAME,DESC", "size,Asc", "SIZE,dEsc", "created_at,asc", "CREATED_AT,desc",
        "updated_at,ASC", "UPDATED_AT,DESC", "type,asc", "TYPE,desc",
    )
    fun documentedAsciiSortTokensAreCaseInsensitive(field: String, direction: String) {
        val parsed =
            parseStorageSelection(
                Parameters.build {
                    append("sortBy", field)
                    append("sortOrder", direction)
                },
                SelectionSurface.FOLDER,
            )
        assertEquals(field.uppercase(), parsed.sortField.name)
        assertEquals(direction.uppercase(), parsed.sortOrder.name)
    }

    @ParameterizedTest(name = "P91-parser-duplicate {0}")
    @ValueSource(strings = ["limit", "offset", "sortBy", "sortOrder"])
    fun duplicateKnownKeysAreRejected(field: String) {
        val failure = assertFailsWith<ValidationException> {
            parseStorageSelection(
                Parameters.build {
                    append(field, "1")
                    append(field, "1")
                },
                SelectionSurface.FOLDER,
            )
        }
        assertEquals(field, failure.field)
    }

    @ParameterizedTest(name = "P91-parser-unsupported {0}/{1}")
    @CsvSource(
        "RECENT,offset", "RECENT,sortBy", "RECENT,sortOrder", "SUGGESTIONS,offset", "SUGGESTIONS,sortBy",
        "SUGGESTIONS,sortOrder", "METADATA,offset", "METADATA,sortBy", "METADATA,sortOrder",
        "SEARCH,sortBy", "SEARCH,sortOrder",
    )
    fun knownUnsupportedControlsCannotBeSilentlyIgnored(surface: SelectionSurface, field: String) {
        val failure =
            assertFailsWith<ValidationException> {
                parseStorageSelection(Parameters.build { append(field, "0") }, surface)
            }
        assertEquals(field, failure.field)
    }

    @ParameterizedTest(name = "P91-parser-field-invalid {0}")
    @ValueSource(strings = ["", "name ", "unknown", "ſize"])
    fun invalidFieldIsSafe(value: String) = denied("sortBy", value)

    @ParameterizedTest(name = "P91-parser-order-invalid {0}")
    @ValueSource(strings = ["", "asc ", "unknown", "dеsc"])
    fun invalidOrderIsSafe(value: String) = denied("sortOrder", value)

    private fun denied(field: String, value: String) {
        val failure =
            assertFailsWith<ValidationException> {
                parseStorageSelection(Parameters.build { append(field, value) }, SelectionSurface.FOLDER)
            }
        assertEquals(field, failure.field)
        if (value.isNotBlank() && value != field) assertFalse(failure.message.contains(value))
    }
}
