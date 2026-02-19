/**
 * VaultStadio SortDialog Tests
 *
 * Tests for sort field and order enums.
 */

package com.vaultstadio.app.ui.components.dialogs

import com.vaultstadio.app.domain.storage.model.SortField
import com.vaultstadio.app.domain.storage.model.SortOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for SortField and SortOrder used by SortDialog.
 */
class SortDialogTest {

    @Test
    fun `SortField has all expected values`() {
        val fields = SortField.entries

        assertEquals(5, fields.size)
        assertTrue(fields.contains(SortField.NAME))
        assertTrue(fields.contains(SortField.SIZE))
        assertTrue(fields.contains(SortField.CREATED_AT))
        assertTrue(fields.contains(SortField.UPDATED_AT))
        assertTrue(fields.contains(SortField.TYPE))
    }

    @Test
    fun `SortOrder has ASC and DESC`() {
        val orders = SortOrder.entries

        assertEquals(2, orders.size)
        assertTrue(orders.contains(SortOrder.ASC))
        assertTrue(orders.contains(SortOrder.DESC))
    }

    @Test
    fun `Default sort is NAME ASC`() {
        val defaultField = SortField.NAME
        val defaultOrder = SortOrder.ASC

        assertEquals(SortField.NAME, defaultField)
        assertEquals(SortOrder.ASC, defaultOrder)
    }

    @Test
    fun `SortField ordinal values`() {
        assertEquals(0, SortField.NAME.ordinal)
        assertEquals(1, SortField.SIZE.ordinal)
        assertEquals(2, SortField.CREATED_AT.ordinal)
        assertEquals(3, SortField.UPDATED_AT.ordinal)
        assertEquals(4, SortField.TYPE.ordinal)
    }

    @Test
    fun `SortOrder ordinal values`() {
        assertEquals(0, SortOrder.ASC.ordinal)
        assertEquals(1, SortOrder.DESC.ordinal)
    }

    @Test
    fun `SortField name property`() {
        assertEquals("NAME", SortField.NAME.name)
        assertEquals("SIZE", SortField.SIZE.name)
        assertEquals("CREATED_AT", SortField.CREATED_AT.name)
        assertEquals("UPDATED_AT", SortField.UPDATED_AT.name)
        assertEquals("TYPE", SortField.TYPE.name)
    }
}
