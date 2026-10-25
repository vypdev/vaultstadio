/**
 * Unit tests for storage domain enums: ViewMode, SortField, SortOrder, ItemType, Visibility.
 */

package com.vaultstadio.app.domain.storage

import com.vaultstadio.app.domain.storage.model.ItemType
import com.vaultstadio.app.domain.storage.model.SortField
import com.vaultstadio.app.domain.storage.model.SortOrder
import com.vaultstadio.app.domain.storage.model.ViewMode
import com.vaultstadio.app.domain.storage.model.Visibility
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ViewModeTest {

    @Test
    fun viewMode_hasGridAndList() {
        val modes = ViewMode.entries
        assertEquals(2, modes.size)
        assertTrue(ViewMode.GRID in modes)
        assertTrue(ViewMode.LIST in modes)
    }

    @Test
    fun viewMode_names() {
        assertEquals("GRID", ViewMode.GRID.name)
        assertEquals("LIST", ViewMode.LIST.name)
    }
}

class SortFieldTest {

    @Test
    fun sortField_hasExpectedValues() {
        val fields = SortField.entries
        assertTrue(SortField.NAME in fields)
        assertTrue(SortField.SIZE in fields)
        assertTrue(SortField.CREATED_AT in fields)
        assertTrue(SortField.UPDATED_AT in fields)
        assertTrue(SortField.TYPE in fields)
        assertEquals(5, fields.size)
    }

    @Test
    fun sortField_nameIsDefault() {
        assertEquals("NAME", SortField.NAME.name)
    }

    @Test
    fun sortField_allNames() {
        assertEquals("SIZE", SortField.SIZE.name)
        assertEquals("CREATED_AT", SortField.CREATED_AT.name)
        assertEquals("UPDATED_AT", SortField.UPDATED_AT.name)
        assertEquals("TYPE", SortField.TYPE.name)
    }
}

class SortOrderTest {

    @Test
    fun sortOrder_hasAscAndDesc() {
        val orders = SortOrder.entries
        assertEquals(2, orders.size)
        assertTrue(SortOrder.ASC in orders)
        assertTrue(SortOrder.DESC in orders)
    }

    @Test
    fun sortOrder_names() {
        assertEquals("ASC", SortOrder.ASC.name)
        assertEquals("DESC", SortOrder.DESC.name)
    }
}

class ItemTypeTest {

    @Test
    fun itemType_hasFileAndFolder() {
        val types = ItemType.entries
        assertEquals(2, types.size)
        assertTrue(ItemType.FILE in types)
        assertTrue(ItemType.FOLDER in types)
    }

    @Test
    fun itemType_names() {
        assertEquals("FILE", ItemType.FILE.name)
        assertEquals("FOLDER", ItemType.FOLDER.name)
    }
}

class VisibilityTest {

    @Test
    fun visibility_hasExpectedValues() {
        val values = Visibility.entries
        assertTrue(Visibility.PRIVATE in values)
        assertTrue(Visibility.SHARED in values)
        assertTrue(Visibility.PUBLIC in values)
        assertEquals(3, values.size)
    }

    @Test
    fun visibility_names() {
        assertEquals("PRIVATE", Visibility.PRIVATE.name)
        assertEquals("SHARED", Visibility.SHARED.name)
        assertEquals("PUBLIC", Visibility.PUBLIC.name)
    }
}
