/**
 * VaultStadio Internationalization (i18n) – String resources contract.
 */

package com.vaultstadio.app.i18n

/**
 * String resources interface. All locale implementations must provide every property.
 */
interface StringResources {
    // App
    val appName: String

    // Navigation
    val navMyFiles: String
    val navRecent: String
    val navStarred: String
    val navShared: String
    val navSharedWithMe: String
    val navTrash: String
    val navSettings: String
    val navProfile: String
    val navAdmin: String
    val navPlugins: String
    val navAI: String
    val navSync: String
    val navFederation: String
    val navCollaboration: String
    val navActivity: String
    val navAdvanced: String

    // Actions
    val actionNew: String
    val actionUpload: String
    val actionUploadFiles: String
    val actionCreateFolder: String
    val actionSelectFiles: String
    val actionAddMoreFiles: String
    val actionCancel: String
    val actionClose: String
    val actionDone: String
    val actionSave: String
    val actionDelete: String
    val actionRename: String
    val actionMove: String
    val actionCopy: String
    val actionShare: String
    val actionDownload: String
    val actionRestore: String
    val actionEmptyTrash: String
    val actionCopyLink: String
    val actionSearch: String
    val actionLogout: String
    val actionAdvancedSearch: String
    val actionRefresh: String

    // File Management
    val filesHome: String
    val filesEmpty: String
    val filesSelectToUpload: String
    val filesOfCompleted: String
    val filesFailed: String
    val folderName: String
    val folderNameError: String
    val folderNameSlashError: String

    // Auth
    val authLogin: String
    val authRegister: String
    val authEmail: String
    val authPassword: String
    val authUsername: String
    val authConfirmPassword: String
    val authForgotPassword: String
    val authNoAccount: String
    val authHaveAccount: String
    val authWelcome: String
    val authSignInToContinue: String
    val authCreateAccount: String
    val authJoinUs: String

    // Settings
    val settingsTitle: String
    val settingsAppearance: String
    val settingsTheme: String
    val settingsThemeLight: String
    val settingsThemeDark: String
    val settingsThemeSystem: String
    val settingsLanguage: String
    val settingsStorage: String
    val settingsStorageUsed: String
    val settingsAccount: String
    val settingsAbout: String
    val settingsVersion: String

    // Sharing
    val shareTitle: String
    val shareCreateLink: String
    val shareExpiration: String
    val sharePassword: String
    val sharePasswordOptional: String
    val shareMaxDownloads: String
    val shareUnlimited: String
    val shareDays: String
    val shareNoShares: String
    val shareNoSharesDesc: String

    // Trash
    val trashEmpty: String
    val trashEmptyDesc: String
    val trashEmptyAction: String
    val trashRestoreItem: String
    val trashDeletePermanently: String
    val trashRestoreConfirmMessage: String
    val trashDeletePermanentlyConfirmMessage: String

    // Errors
    val errorGeneric: String
    val errorNetwork: String
    val errorAuth: String
    val errorNotFound: String
    val errorUpload: String

    // Time
    val timeJustNow: String
    val timeMinuteAgo: String
    val timeMinutesAgo: String
    val timeHourAgo: String
    val timeHoursAgo: String
    val timeYesterday: String
    val timeDaysAgo: String
    val timeWeeksAgo: String
    val timeMonthsAgo: String
    val timeYearsAgo: String

    // Storage
    val storageBytes: String
    val storageKB: String
    val storageMB: String
    val storageGB: String
    val storageTB: String
    val storageFree: String
    val storageUsed: String

    // AI
    val aiAssistant: String
    val aiChat: String
    val aiDescribe: String
    val aiTag: String
    val aiClassify: String
    val aiSummarize: String
    val aiProviders: String
    val aiModels: String
    val aiNoProvider: String
    val aiConfigureProvider: String
    val aiThinking: String
    val aiSend: String

    // Versioning
    val versionHistory: String
    val versionRestore: String
    val versionCompare: String
    val versionDownload: String
    val versionCleanup: String
    val versionCurrent: String
    val versionRestored: String
    val versionNoHistory: String

    // Sync
    val syncDevices: String
    val syncLastSync: String
    val syncConflicts: String
    val syncResolve: String
    val syncDeactivate: String
    val syncRemove: String
    val syncNoDevices: String
    val syncNoConflicts: String
    val syncKeepLocal: String
    val syncKeepRemote: String
    val syncKeepBoth: String

    // Collaboration
    val collaborationJoin: String
    val collaborationLeave: String
    val collaborationParticipants: String
    val collaborationComments: String
    val collaborationSave: String
    val collaborationResolve: String
    val collaborationReply: String
    val collaborationNoComments: String

    // Federation
    val federationInstances: String
    val federationRequest: String
    val federationShares: String
    val federationIdentities: String
    val federationActivities: String
    val federationBlock: String
    val federationAccept: String
    val federationDecline: String
    val federationRevoke: String
    val federationNoInstances: String

    // Activity
    val activityRecent: String
    val activityFilter: String
    val activityNoActivity: String

    // Metadata
    val metadataDetails: String
    val metadataImage: String
    val metadataVideo: String
    val metadataDocument: String
    val metadataCamera: String
    val metadataLocation: String

    // Search
    val searchAdvanced: String
    val searchByContent: String
    val searchByMetadata: String
    val searchFileType: String
    val searchDateRange: String
    val searchSizeRange: String
    val searchReset: String

    // Settings - Additional
    val settingsAutoSync: String
    val settingsAutoSyncDesc: String
    val settingsClearCache: String
    val settingsClearCacheDesc: String
    val settingsNotifications: String
    val settingsPushNotifications: String
    val settingsPushNotificationsDesc: String
    val settingsSecurity: String
    val settingsChangePassword: String
    val settingsChangePasswordDesc: String
    val settingsLicenses: String
    val settingsLicensesDesc: String
    val settingsSignOutDesc: String
    val settingsLogoutConfirm: String

    // AI - Additional
    val aiStartConversation: String
    val aiStartConversationDesc: String
    val aiDescribeImage: String
    val aiDescribeImageDesc: String
    val aiAutoTagContent: String
    val aiAutoTagContentDesc: String
    val aiSummarizeText: String
    val aiSummarizeTextDesc: String
    val aiNoProviderConfigured: String
    val aiGeneratedTags: String
    val aiSelectModel: String
    val aiTypePlaceholder: String
    val aiImageUrlPlaceholder: String
    val aiTagPlaceholder: String
    val aiClassifyPlaceholder: String
    val aiSummarizePlaceholder: String
    val aiCouldNotProcess: String
    val aiProvider: String

    // Federation - Additional
    val federationTitle: String
    val federationEnterDomain: String
    val federationInstanceDomain: String
    val federationInstanceDomainPlaceholder: String
    val federationMessageOptional: String
    val federationBlockConfirm: String
    val federationNoSharesDesc: String
    val federationIncomingShares: String
    val federationOutgoingShares: String
    val federationNoLinkedIdentities: String
    val federationLinkIdentityDesc: String
    val federationNoInstancesDesc: String

    // Collaboration - Additional
    val collaborationJoiningSession: String
    val collaborationCouldNotJoin: String
    val collaborationRetry: String
    val collaborationAddComment: String
    val collaborationNoCommentsYet: String
    val collaborationComment: String
    val collaborationAdd: String
    val collaborationVersion: String
    val collaborationLastSaved: String
    val collaborationSelectFileToStart: String

    // Sync - Additional
    val syncTitle: String
    val syncDeactivateConfirm: String
    val syncRemoveConfirm: String
    val syncResolveConflict: String
    val syncChooseResolution: String
    val syncKeepLocalVersion: String
    val syncKeepRemoteVersion: String
    val syncKeepBothRename: String
    val syncMergeChanges: String
    val syncResolveManually: String
    val syncNoDevicesDesc: String
    val syncAllInSync: String
    val syncInactive: String

    // Version - Additional
    val versionTitle: String
    val versionTotalSize: String
    val versionVersions: String
    val versionRestoreConfirm: String
    val versionRestoreDesc: String
    val versionCommentOptional: String
    val versionCleanupOld: String
    val versionCleanupDesc: String
    val versionKeepLast: String
    val versionKeepLastPlaceholder: String
    val versionDeleteOlderThan: String
    val versionDeleteOlderThanPlaceholder: String

    // File Info - Additional
    val infoType: String
    val infoFolder: String
    val infoFile: String
    val infoSize: String
    val infoMimeType: String
    val infoUnknown: String
    val infoCreated: String
    val infoModified: String
    val infoPath: String
    val infoVisibility: String
    val infoMoveTo: String
    val infoMakeCopy: String
    val infoMoveToTrash: String

    // Common
    val commonBack: String
    val commonRequest: String
    val commonAccept: String
    val commonDecline: String
    val commonRevoke: String
    val commonBlock: String
    val commonRemove: String
    val commonDeactivate: String
    val commonResolve: String
    val commonAdd: String
    val commonSend: String
    val commonRetry: String
    val commonUnlink: String
    val commonConfigure: String
    val commonSettings: String
    val commonPlay: String

    // Errors - Additional
    val errorPasswordsDoNotMatch: String
    val errorEmailPasswordRequired: String
    val errorAllFieldsRequired: String
    val errorPasswordTooShort: String

    // Navigation - Additional
    val navHome: String
}
