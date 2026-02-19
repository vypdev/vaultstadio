/**
 * Unit tests for Security screen logic: sessions, login history, and security settings.
 */

package com.vaultstadio.app.ui.screens

import com.vaultstadio.app.domain.auth.model.ActiveSession
import com.vaultstadio.app.domain.auth.model.LoginEvent
import com.vaultstadio.app.domain.auth.model.SecuritySettings
import com.vaultstadio.app.domain.auth.model.SessionDeviceType
import com.vaultstadio.app.domain.auth.model.TwoFactorMethod
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecurityScreenTest {

    @Test
    fun activeSession_holdsDeviceAndCurrentFlag() {
        val now = Clock.System.now()
        val session = ActiveSession(
            id = "s1",
            deviceName = "Chrome on Windows",
            deviceType = SessionDeviceType.WEB,
            lastActiveAt = now,
            location = "Berlin",
            ipAddress = "192.168.1.1",
            isCurrent = true,
        )
        assertTrue(session.isCurrent)
        assertEquals("Chrome on Windows", session.deviceName)
        assertEquals(SessionDeviceType.WEB, session.deviceType)
    }

    @Test
    fun securityScreen_filtersCurrentSession() {
        val now = Clock.System.now()
        val sessions = listOf(
            ActiveSession("1", "Device A", SessionDeviceType.DESKTOP, now, null, null, true),
            ActiveSession("2", "Device B", SessionDeviceType.MOBILE, now, null, null, false),
        )
        val current = requireNotNull(sessions.singleOrNull { it.isCurrent })
        assertEquals("Device A", current.deviceName)
    }

    @Test
    fun loginEvent_holdsSuccessAndTimestamp() {
        val now = Clock.System.now()
        val event = LoginEvent(
            id = "e1",
            timestamp = now,
            ipAddress = "10.0.0.1",
            location = null,
            deviceInfo = "Chrome",
            success = true,
        )
        assertTrue(event.success)
        assertEquals(now, event.timestamp)
    }

    @Test
    fun securitySettings_twoFactorDisabledByDefault() {
        val settings = SecuritySettings(twoFactorEnabled = false, twoFactorMethod = null)
        assertFalse(settings.twoFactorEnabled)
        assertNull(settings.twoFactorMethod)
    }

    @Test
    fun securitySettings_twoFactorMethodValues() {
        val methods = TwoFactorMethod.entries
        assertTrue(methods.contains(TwoFactorMethod.TOTP))
        assertTrue(methods.contains(TwoFactorMethod.SMS))
        assertTrue(methods.contains(TwoFactorMethod.EMAIL))
    }

    @Test
    fun sessionDeviceType_hasExpectedValues() {
        val types = SessionDeviceType.entries
        assertTrue(SessionDeviceType.WEB in types)
        assertTrue(SessionDeviceType.MOBILE in types)
        assertTrue(SessionDeviceType.DESKTOP in types)
        assertTrue(SessionDeviceType.UNKNOWN in types)
    }
}
