/**
 * Tests for internationalization strings.
 */

package com.vaultstadio.app.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StringsTest {

    @Test
    fun language_hasAllExpectedLanguages() {
        val languages = Language.entries
        assertEquals(7, languages.size)
        assertTrue(languages.contains(Language.ENGLISH))
        assertTrue(languages.contains(Language.SPANISH))
        assertTrue(languages.contains(Language.FRENCH))
        assertTrue(languages.contains(Language.GERMAN))
        assertTrue(languages.contains(Language.PORTUGUESE))
        assertTrue(languages.contains(Language.CHINESE))
        assertTrue(languages.contains(Language.JAPANESE))
    }

    @Test
    fun language_hasCorrectCodes() {
        assertEquals("en", Language.ENGLISH.code)
        assertEquals("es", Language.SPANISH.code)
        assertEquals("fr", Language.FRENCH.code)
        assertEquals("de", Language.GERMAN.code)
        assertEquals("pt", Language.PORTUGUESE.code)
        assertEquals("zh", Language.CHINESE.code)
        assertEquals("ja", Language.JAPANESE.code)
    }

    @Test
    fun language_hasDisplayNames() {
        assertEquals("English", Language.ENGLISH.displayName)
        assertEquals("Español", Language.SPANISH.displayName)
        assertEquals("Français", Language.FRENCH.displayName)
        assertEquals("Deutsch", Language.GERMAN.displayName)
        assertEquals("Português", Language.PORTUGUESE.displayName)
        assertEquals("中文", Language.CHINESE.displayName)
        assertEquals("日本語", Language.JAPANESE.displayName)
    }

    @Test
    fun englishStrings_hasAppName() {
        val strings = EnglishStrings
        assertEquals("VaultStadio", strings.appName)
    }

    @Test
    fun spanishStrings_hasAppName() {
        val strings = SpanishStrings
        assertEquals("VaultStadio", strings.appName)
    }

    @Test
    fun englishStrings_hasNavigationStrings() {
        val strings = EnglishStrings
        assertNotNull(strings.navMyFiles)
        assertNotNull(strings.navRecent)
        assertNotNull(strings.navStarred)
        assertNotNull(strings.navShared)
        assertNotNull(strings.navTrash)
        assertNotNull(strings.navSettings)
    }

    @Test
    fun englishStrings_hasActionStrings() {
        val strings = EnglishStrings
        assertNotNull(strings.actionNew)
        assertNotNull(strings.actionUpload)
        assertNotNull(strings.actionCreateFolder)
        assertNotNull(strings.actionCancel)
        assertNotNull(strings.actionDelete)
        assertNotNull(strings.actionRename)
    }

    @Test
    fun englishStrings_hasAIStrings() {
        val strings = EnglishStrings
        assertNotNull(strings.aiAssistant)
        assertNotNull(strings.aiChat)
        assertNotNull(strings.aiDescribe)
        assertNotNull(strings.aiTag)
        assertNotNull(strings.aiSummarize)
        assertNotNull(strings.aiStartConversation)
        assertNotNull(strings.aiNoProviderConfigured)
    }

    @Test
    fun englishStrings_hasFederationStrings() {
        val strings = EnglishStrings
        assertNotNull(strings.federationTitle)
        assertNotNull(strings.federationInstances)
        assertNotNull(strings.federationShares)
        assertNotNull(strings.federationIdentities)
        assertNotNull(strings.federationRequest)
    }

    @Test
    fun englishStrings_hasSyncStrings() {
        val strings = EnglishStrings
        assertNotNull(strings.syncTitle)
        assertNotNull(strings.syncDevices)
        assertNotNull(strings.syncConflicts)
        assertNotNull(strings.syncResolve)
        assertNotNull(strings.syncKeepLocal)
        assertNotNull(strings.syncKeepRemote)
    }

    @Test
    fun englishStrings_hasVersionStrings() {
        val strings = EnglishStrings
        assertNotNull(strings.versionTitle)
        assertNotNull(strings.versionHistory)
        assertNotNull(strings.versionRestore)
        assertNotNull(strings.versionCleanup)
    }

    @Test
    fun englishStrings_hasCollaborationStrings() {
        val strings = EnglishStrings
        assertNotNull(strings.collaborationJoin)
        assertNotNull(strings.collaborationLeave)
        assertNotNull(strings.collaborationParticipants)
        assertNotNull(strings.collaborationComments)
        assertNotNull(strings.collaborationAddComment)
    }

    @Test
    fun englishStrings_hasSettingsStrings() {
        val strings = EnglishStrings
        assertNotNull(strings.settingsTitle)
        assertNotNull(strings.settingsTheme)
        assertNotNull(strings.settingsLanguage)
        assertNotNull(strings.settingsAutoSync)
        assertNotNull(strings.settingsClearCache)
        assertNotNull(strings.settingsChangePassword)
    }

    @Test
    fun englishStrings_hasCommonStrings() {
        val strings = EnglishStrings
        assertNotNull(strings.commonBack)
        assertNotNull(strings.commonRequest)
        assertNotNull(strings.commonAccept)
        assertNotNull(strings.commonDecline)
        assertNotNull(strings.commonSend)
        assertNotNull(strings.commonRetry)
    }

    @Test
    fun allLanguages_haveConsistentStrings() {
        val allStrings = listOf(
            EnglishStrings,
            SpanishStrings,
            FrenchStrings,
            GermanStrings,
            PortugueseStrings,
            ChineseStrings,
            JapaneseStrings,
        )

        allStrings.forEach { strings ->
            // Verify all critical strings are non-empty
            assertTrue(strings.appName.isNotEmpty())
            assertTrue(strings.navMyFiles.isNotEmpty())
            assertTrue(strings.actionUpload.isNotEmpty())
            assertTrue(strings.settingsTitle.isNotEmpty())
            assertTrue(strings.aiAssistant.isNotEmpty())
        }
    }

    @Test
    fun stringsObject_defaultsToEnglish() {
        // Reset to default
        Strings.currentLanguage = Language.ENGLISH

        val resources = Strings.resources
        assertEquals(EnglishStrings.appName, resources.appName)
    }

    @Test
    fun stringsObject_switchesLanguage() {
        Strings.currentLanguage = Language.SPANISH
        assertEquals(SpanishStrings.appName, Strings.resources.appName)

        Strings.currentLanguage = Language.FRENCH
        assertEquals(FrenchStrings.appName, Strings.resources.appName)

        // Reset to English
        Strings.currentLanguage = Language.ENGLISH
    }
}
