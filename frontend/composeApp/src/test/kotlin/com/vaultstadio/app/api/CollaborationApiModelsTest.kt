/**
 * VaultStadio Collaboration API Models Tests
 */

package com.vaultstadio.app.api

import com.vaultstadio.app.domain.collaboration.model.PresenceStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CollaborationApiModelsTest {

    @Test
    fun testPresenceStatusOnline() {
        val status = PresenceStatus.ONLINE
        assertEquals("ONLINE", status.name)
    }

    @Test
    fun testPresenceStatusAway() {
        val status = PresenceStatus.AWAY
        assertEquals("AWAY", status.name)
    }

    @Test
    fun testPresenceStatusBusy() {
        val status = PresenceStatus.BUSY
        assertEquals("BUSY", status.name)
    }

    @Test
    fun testPresenceStatusOffline() {
        val status = PresenceStatus.OFFLINE
        assertEquals("OFFLINE", status.name)
    }

    @Test
    fun testPresenceStatusValues() {
        val statuses = PresenceStatus.entries

        assertEquals(4, statuses.size)
        assertTrue(statuses.contains(PresenceStatus.ONLINE))
        assertTrue(statuses.contains(PresenceStatus.AWAY))
        assertTrue(statuses.contains(PresenceStatus.BUSY))
        assertTrue(statuses.contains(PresenceStatus.OFFLINE))
    }

    @Test
    fun testPresenceStatusOrdering() {
        val statuses = PresenceStatus.entries

        // Verify the enum values exist in expected positions
        assertEquals(PresenceStatus.ONLINE, statuses[0])
        assertEquals(PresenceStatus.AWAY, statuses[1])
        assertEquals(PresenceStatus.BUSY, statuses[2])
        assertEquals(PresenceStatus.OFFLINE, statuses[3])
    }
}
