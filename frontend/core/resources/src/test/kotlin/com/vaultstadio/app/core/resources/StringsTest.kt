/**
 * Tests for internationalization strings.
 */

package com.vaultstadio.app.core.resources

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
        assertNotNull(strings.navSharedWithMe)
        assertNotNull(strings.navTrash)
        assertNotNull(strings.navSettings)
        assertNotNull(strings.navHome)
        assertNotNull(strings.navProfile)
        assertNotNull(strings.navAdmin)
        assertNotNull(strings.navPlugins)
        assertNotNull(strings.navSync)
    }

    @Test
    fun allLanguages_haveProfileAndAdminNav() {
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
            assertTrue(strings.navProfile.isNotEmpty(), "navProfile should be non-empty for all languages")
            assertTrue(strings.navAdmin.isNotEmpty(), "navAdmin should be non-empty for all languages")
        }
    }

    @Test
    fun allLanguages_haveSyncAndPluginsNav() {
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
            assertTrue(strings.navSync.isNotEmpty(), "navSync should be non-empty for all languages")
            assertTrue(strings.navPlugins.isNotEmpty(), "navPlugins should be non-empty for all languages")
        }
    }

    @Test
    fun allLanguages_haveNavTrashAndActionCancel() {
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
            assertTrue(strings.navTrash.isNotEmpty(), "navTrash should be non-empty for all languages")
            assertTrue(strings.actionCancel.isNotEmpty(), "actionCancel should be non-empty for all languages")
        }
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
        assertNotNull(strings.settingsSecurity)
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
            assertTrue(strings.appName.isNotEmpty())
            assertTrue(strings.navMyFiles.isNotEmpty())
            assertTrue(strings.navHome.isNotEmpty())
            assertTrue(strings.actionUpload.isNotEmpty())
            assertTrue(strings.settingsTitle.isNotEmpty())
            assertTrue(strings.settingsSecurity.isNotEmpty())
            assertTrue(strings.aiAssistant.isNotEmpty())
        }
    }

    @Test
    fun englishStrings_hasAuthStrings() {
        val strings = EnglishStrings
        assertNotNull(strings.authLogin)
        assertNotNull(strings.authRegister)
        assertNotNull(strings.authEmail)
        assertNotNull(strings.authPassword)
        assertNotNull(strings.authUsername)
        assertNotNull(strings.authConfirmPassword)
        assertNotNull(strings.authWelcome)
    }

    @Test
    fun allLanguages_haveNavHome() {
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
            assertTrue(strings.navHome.isNotEmpty(), "navHome should be non-empty for all languages")
        }
    }

    @Test
    fun allLanguages_haveErrorGeneric() {
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
            assertTrue(strings.errorGeneric.isNotEmpty(), "errorGeneric should be non-empty for all languages")
        }
    }

    @Test
    fun allLanguages_haveAppName() {
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
            assertTrue(strings.appName.isNotEmpty(), "appName should be non-empty for all languages")
        }
    }

    @Test
    fun stringsObject_defaultsToEnglish() {
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

        Strings.currentLanguage = Language.ENGLISH
    }

    @Test
    fun loadSavedLanguage_withValidCode_setsLanguage() {
        Strings.currentLanguage = Language.ENGLISH
        Strings.loadSavedLanguage("es")
        assertEquals(Language.SPANISH, Strings.currentLanguage)
        assertEquals(SpanishStrings.appName, Strings.resources.appName)

        Strings.loadSavedLanguage("de")
        assertEquals(Language.GERMAN, Strings.currentLanguage)
        Strings.currentLanguage = Language.ENGLISH
    }

    @Test
    fun loadSavedLanguage_withNull_keepsCurrent() {
        Strings.currentLanguage = Language.FRENCH
        Strings.loadSavedLanguage(null)
        assertEquals(Language.FRENCH, Strings.currentLanguage)
        Strings.currentLanguage = Language.ENGLISH
    }

    @Test
    fun loadSavedLanguage_withUnknownCode_fallsBackToEnglish() {
        Strings.currentLanguage = Language.SPANISH
        Strings.loadSavedLanguage("xx")
        assertEquals(Language.ENGLISH, Strings.currentLanguage)
    }

    @Test
    fun allLanguages_haveActionSave() {
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
            assertTrue(strings.actionSave.isNotEmpty(), "actionSave should be non-empty for all languages")
        }
    }

    @Test
    fun allLanguages_haveNavSettings() {
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
            assertTrue(strings.navSettings.isNotEmpty(), "navSettings should be non-empty for all languages")
        }
    }

    @Test
    fun allLanguages_haveActionDelete() {
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
            assertTrue(strings.actionDelete.isNotEmpty(), "actionDelete should be non-empty for all languages")
        }
    }

    @Test
    fun allLanguages_haveActionRename() {
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
            assertTrue(strings.actionRename.isNotEmpty(), "actionRename should be non-empty for all languages")
        }
    }

    @Test
    fun allLanguages_haveActionClose() {
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
            assertTrue(strings.actionClose.isNotEmpty(), "actionClose should be non-empty for all languages")
        }
    }

    @Test
    fun allLanguages_haveActionCopyAndActionMove() {
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
            assertTrue(strings.actionCopy.isNotEmpty(), "actionCopy should be non-empty for all languages")
            assertTrue(strings.actionMove.isNotEmpty(), "actionMove should be non-empty for all languages")
        }
    }

    @Test
    fun allLanguages_haveActionCopyLink() {
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
            assertTrue(strings.actionCopyLink.isNotEmpty(), "actionCopyLink should be non-empty for all languages")
        }
    }

    @Test
    fun allLanguages_haveActionDownload() {
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
            assertTrue(strings.actionDownload.isNotEmpty(), "actionDownload should be non-empty for all languages")
        }
    }

    @Test
    fun allLanguages_haveActionShare() {
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
            assertTrue(strings.actionShare.isNotEmpty(), "actionShare should be non-empty for all languages")
        }
    }

    @Test
    fun allLanguages_haveShareTitle() {
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
            assertTrue(strings.shareTitle.isNotEmpty(), "shareTitle should be non-empty for all languages")
        }
    }

    @Test
    fun allLanguages_haveNavRecentAndNavStarred() {
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
            assertTrue(strings.navRecent.isNotEmpty(), "navRecent should be non-empty for all languages")
            assertTrue(strings.navStarred.isNotEmpty(), "navStarred should be non-empty for all languages")
        }
    }

    @Test
    fun allLanguages_haveVersionTitleAndSyncTitle() {
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
            assertTrue(strings.versionTitle.isNotEmpty(), "versionTitle should be non-empty for all languages")
            assertTrue(strings.syncTitle.isNotEmpty(), "syncTitle should be non-empty for all languages")
        }
    }

    @Test
    fun allLanguages_haveActivityNoActivity() {
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
            assertTrue(strings.activityNoActivity.isNotEmpty(), "activityNoActivity should be non-empty for all languages")
        }
    }

    @Test
    fun allLanguages_haveActionRestoreAndActionNew() {
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
            assertTrue(strings.actionRestore.isNotEmpty(), "actionRestore should be non-empty for all languages")
            assertTrue(strings.actionNew.isNotEmpty(), "actionNew should be non-empty for all languages")
        }
    }

    @Test
    fun allLanguages_haveCommonBackAndCommonRetry() {
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
            assertTrue(strings.commonBack.isNotEmpty(), "commonBack should be non-empty for all languages")
            assertTrue(strings.commonRetry.isNotEmpty(), "commonRetry should be non-empty for all languages")
        }
    }
}
