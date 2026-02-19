/**
 * Platform-specific URL/path sync for navigation.
 *
 * On web: reads and updates the browser URL (pathname) and listens to popstate for back/forward.
 * On other platforms: no-op for URL; getInitialPath() returns default path for consistency.
 */

package com.vaultstadio.app.platform

/**
 * Returns the current path for routing (e.g. browser pathname on web, default path elsewhere).
 */
expect fun getInitialPath(): String

/**
 * Updates the current route path (e.g. browser URL on web; no-op on other platforms).
 */
expect fun setPath(path: String)

/**
 * Registers a callback for back/forward navigation (e.g. browser popstate on web; no-op elsewhere).
 */
expect fun setOnPopState(callback: () -> Unit)
