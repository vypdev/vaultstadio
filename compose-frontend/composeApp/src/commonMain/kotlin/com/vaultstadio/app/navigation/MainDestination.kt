package com.vaultstadio.app.navigation

/**
 * All possible destinations within the main authenticated area.
 */
enum class MainDestination {
    // Core file management
    FILES,
    RECENT,
    STARRED,
    TRASH,

    // Sharing
    SHARED,
    SHARED_WITH_ME,

    // User
    SETTINGS,
    PROFILE,

    // Admin
    ADMIN,
    ACTIVITY,
    PLUGINS,

    // Advanced features (Phase 6)
    AI,
    SYNC,
    FEDERATION,
    COLLABORATION,
    VERSION_HISTORY,

    // Account & Security
    CHANGE_PASSWORD,
    SECURITY,
    LICENSES,
}
