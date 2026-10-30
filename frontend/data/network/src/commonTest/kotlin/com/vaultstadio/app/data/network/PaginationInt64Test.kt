package com.vaultstadio.app.data.network

import com.vaultstadio.app.data.network.dto.common.PaginatedResponseDTO
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class PaginationInt64Test {
    private val json = HttpClientFactory.getJson()
    private val serializer = PaginatedResponseDTO.serializer(String.serializer())

    @Test fun ordinaryWireValuesRetainTheirMeaning() {
        val page = decode("1", "2")
        assertEquals(1L, page.page)
        assertEquals(2L, page.totalPages)
        assertEquals(listOf("item"), page.items)
    }

    @Test fun pageBeyondIntRangeDoesNotWrap() {
        assertEquals(2147483648L, decode("2147483648", "2147483648").page)
    }

    @Test fun largestLongPageCountDecodesExactly() {
        assertEquals(Long.MAX_VALUE, decode("1", "9223372036854775807").totalPages)
    }

    @Test fun serializationKeepsNumericFieldsAndNames() {
        val encoded = json.encodeToString(serializer, decode("2147483648", "9223372036854775807"))
        val fields = json.parseToJsonElement(encoded).jsonObject
        assertEquals(setOf("items", "total", "page", "pageSize", "totalPages", "hasMore"), fields.keys)
        assertFalse(fields.getValue("page").jsonPrimitive.isString)
        assertFalse(fields.getValue("totalPages").jsonPrimitive.isString)
        assertEquals(Long.MAX_VALUE, fields.getValue("totalPages").jsonPrimitive.long)
    }

    @Test fun extremeMetadataSurvivesTheActualClientRoundTrip() {
        val original = decode("2147483648", "9223372036854775807")
        assertEquals(original, json.decodeFromString(serializer, json.encodeToString(serializer, original)))
    }

    @Test fun outOfLongRangeMetadataFailsInsteadOfWrapping() {
        assertFailsWith<IllegalArgumentException> { decode("1", "9223372036854775808") }
    }

    @Test fun fractionalPageIsNotAnIntegerPage() {
        assertFailsWith<IllegalArgumentException> { decode("1.5", "2") }
    }

    @Test fun unknownFutureFieldsDoNotCorruptLongMetadata() {
        val payload = payload("2147483648", "9223372036854775807").dropLast(1) + ",\"future\":true}"
        val page = json.decodeFromString(serializer, payload)
        assertEquals(2147483648L, page.page)
        assertEquals(Long.MAX_VALUE, page.totalPages)
    }

    private fun decode(page: String, pages: String) = json.decodeFromString(serializer, payload(page, pages))

    private fun payload(page: String, pages: String) =
        """
        {"items":["item"],"total":9223372036854775807,
         "page":$page,"pageSize":1,"totalPages":$pages,"hasMore":true}
        """.trimIndent()
}
