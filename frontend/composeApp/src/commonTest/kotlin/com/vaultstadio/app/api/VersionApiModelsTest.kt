/**
 * VaultStadio Version API Models Tests
 */

package com.vaultstadio.app.api

import com.vaultstadio.app.domain.model.RestoreVersionRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VersionApiModelsTest {

    @Test
    fun testRestoreVersionRequestCreation() {
        val request = RestoreVersionRequest(
            versionNumber = 5,
            comment = "Restoring to stable version",
        )

        assertEquals(5, request.versionNumber)
        assertEquals("Restoring to stable version", request.comment)
    }

    @Test
    fun testRestoreVersionRequestWithoutComment() {
        val request = RestoreVersionRequest(
            versionNumber = 3,
        )

        assertEquals(3, request.versionNumber)
        assertNull(request.comment)
    }

    @Test
    fun testRestoreVersionRequestWithEmptyComment() {
        val request = RestoreVersionRequest(
            versionNumber = 7,
            comment = "",
        )

        assertEquals(7, request.versionNumber)
        assertEquals("", request.comment)
    }

    @Test
    fun testRestoreVersionRequestDifferentVersionNumbers() {
        val request1 = RestoreVersionRequest(versionNumber = 1)
        val request2 = RestoreVersionRequest(versionNumber = 100)
        val request3 = RestoreVersionRequest(versionNumber = 999)

        assertEquals(1, request1.versionNumber)
        assertEquals(100, request2.versionNumber)
        assertEquals(999, request3.versionNumber)
    }

    @Test
    fun testRestoreVersionRequestWithLongComment() {
        val longComment = "This is a very long comment that explains why we are restoring " +
            "to this particular version. The previous version had some bugs that " +
            "needed to be fixed, so we're rolling back to a known good state."

        val request = RestoreVersionRequest(
            versionNumber = 2,
            comment = longComment,
        )

        assertEquals(2, request.versionNumber)
        assertEquals(longComment, request.comment)
    }
}
