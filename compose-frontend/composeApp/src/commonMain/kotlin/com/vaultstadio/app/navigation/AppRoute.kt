package com.vaultstadio.app.navigation

/**
 * Declarative route definition (go_router style).
 *
 * Each screen declares its [path] segment and optional [parent]; full path and back stack
 * are derived automatically. Optional [pathParam] (e.g. "path") makes this route match
 * the base path plus any remaining segments, captured as a list (splat / catch-all).
 * Example: path = "files", pathParam = "path" matches /files, /files/folder, /files/a/b/file.mp3.
 */
data class AppRoute(
    val destination: MainDestination,
    /** Path segment (e.g. "settings", "change-password"). Use "files" for the root route. */
    val path: String,
    /** Parent destination; null for root-level routes. Used for back stack only. */
    val parent: MainDestination? = null,
    /**
     * If set, this route captures all remaining path segments under this name (splat).
     * Used e.g. for /files/... to navigate the file tree (param name e.g. "path").
     */
    val pathParam: String? = null,
    /**
     * If set, use this as the full URL path instead of building from parent.
     * Allows e.g. SETTINGS to live at /settings while having parent FILES for back stack.
     */
    val fullPathOverride: String? = null,
)

/**
 * Registry of all app routes. Each screen declares its path and parent here.
 * Full paths and navigation stack are computed from this tree.
 */
object AppRoutes {

    /** Path for the auth (login) screen, outside the main route tree. */
    const val AUTH_PATH = "/login"

    /** Default path when no route matches (main area root). */
    const val DEFAULT_MAIN_PATH = "/files"

    /**
     * All route definitions: each destination declares its path segment and parent.
     * Order does not matter; hierarchy is given by [AppRoute.parent].
     */
    val all: List<AppRoute> = listOf(
        // Root (with pathParam so /files, /files/folder/file.mp3 all match)
        AppRoute(MainDestination.FILES, "files", parent = null, pathParam = "path"),
        // Core file management (short URLs; parent FILES for back stack)
        AppRoute(MainDestination.RECENT, "recent", MainDestination.FILES, fullPathOverride = "/recent"),
        AppRoute(MainDestination.STARRED, "starred", MainDestination.FILES, fullPathOverride = "/starred"),
        AppRoute(MainDestination.TRASH, "trash", MainDestination.FILES, fullPathOverride = "/trash"),
        AppRoute(MainDestination.SHARED, "shared", MainDestination.FILES, fullPathOverride = "/shared"),
        AppRoute(
            MainDestination.SHARED_WITH_ME,
            "shared-with-me",
            MainDestination.FILES,
            fullPathOverride = "/shared-with-me",
        ),
        AppRoute(MainDestination.SETTINGS, "settings", MainDestination.FILES, fullPathOverride = "/settings"),
        AppRoute(MainDestination.PROFILE, "profile", MainDestination.FILES, fullPathOverride = "/profile"),
        AppRoute(MainDestination.CHANGE_PASSWORD, "change-password", MainDestination.SETTINGS),
        AppRoute(MainDestination.LICENSES, "licenses", MainDestination.SETTINGS),
        AppRoute(MainDestination.SECURITY, "security", MainDestination.PROFILE),
        AppRoute(MainDestination.ADMIN, "admin", MainDestination.FILES, fullPathOverride = "/admin"),
        AppRoute(MainDestination.ACTIVITY, "activity", MainDestination.FILES, fullPathOverride = "/activity"),
        AppRoute(MainDestination.PLUGINS, "plugins", MainDestination.FILES, fullPathOverride = "/plugins"),
        AppRoute(MainDestination.AI, "ai", MainDestination.FILES, fullPathOverride = "/ai"),
        AppRoute(MainDestination.SYNC, "sync", MainDestination.FILES, fullPathOverride = "/sync"),
        AppRoute(MainDestination.FEDERATION, "federation", MainDestination.FILES, fullPathOverride = "/federation"),
        AppRoute(
            MainDestination.COLLABORATION,
            "collaboration",
            MainDestination.FILES,
            fullPathOverride = "/collaboration",
        ),
        AppRoute(
            MainDestination.VERSION_HISTORY,
            "version-history",
            MainDestination.FILES,
            fullPathOverride = "/version-history",
        ),
    )

    private val byDestination: Map<MainDestination, AppRoute> = all.associateBy { it.destination }

    private fun normalize(path: String): String =
        path.trim().lowercase().removeSuffix("/").ifEmpty { "/" }

    /**
     * Base path for a destination (no path param segments). FILES -> "/files", SETTINGS -> "/settings".
     */
    fun fullPath(destination: MainDestination): String {
        val route = byDestination[destination] ?: return DEFAULT_MAIN_PATH
        route.fullPathOverride?.let { return it }
        val segment = route.path
        val p = route.parent
        return if (p == null) {
            if (segment == "/" || segment.isEmpty()) "/" else "/$segment"
        } else {
            val parentPath = fullPath(p)
            val base = parentPath.trimEnd('/')
            if (base.isEmpty()) "/$segment" else "$base/$segment"
        }
    }

    /** Canonical URL path for a destination, optionally with path params (e.g. file path segments). */
    fun toPath(destination: MainDestination, pathParams: Map<String, List<String>>? = null): String {
        val base = if (destination == MainDestination.FILES) "/files" else fullPath(destination)
        val segments = pathParams?.get("path")?.filter { it.isNotEmpty() }
        return if (!segments.isNullOrEmpty()) "$base/${segments.joinToString("/")}" else base
    }

    /** Destination for a path, or null if no route matches. Root accepts "", "/", "/files", /files/... */
    fun fromPath(path: String): MainDestination? = parseRoute(path)?.destination

    /**
     * Matches a path and returns destination plus any path params (go_router style).
     * Routes with [AppRoute.pathParam] match the base path plus remaining segments (splat).
     */
    fun parseRoute(path: String): RouteMatch? {
        val n = normalize(path)
        if (n == "/" || n.isEmpty()) return RouteMatch(MainDestination.FILES)
        // Exact match first
        for (route in all) {
            val full = normalize(fullPath(route.destination))
            if (route.pathParam == null && full == n) return RouteMatch(route.destination)
        }
        // Splat match: base path + optional segments
        for (route in all) {
            val paramName = route.pathParam ?: continue
            val base = fullPath(route.destination)
            val baseNorm = normalize(base)
            when {
                n == baseNorm -> return RouteMatch(route.destination, emptyMap())
                n.startsWith("$baseNorm/") -> {
                    val rest = n.removePrefix("$baseNorm/").trimStart('/')
                    val segments = rest.split('/').filter { it.isNotEmpty() }
                    return RouteMatch(route.destination, mapOf(paramName to segments))
                }
            }
        }
        return null
    }

    /** Navigation stack for a path (root first, then ancestors, then leaf). */
    fun getStack(path: String): List<MainDestination> {
        val dest = fromPath(path) ?: return listOf(MainDestination.FILES)
        return ancestryStack(dest)
    }

    private fun ancestryStack(destination: MainDestination): List<MainDestination> {
        val route = byDestination[destination] ?: return listOf(destination)
        val parent = route.parent
        return if (parent == null) {
            listOf(destination)
        } else {
            ancestryStack(parent) + destination
        }
    }

    fun isAuthPath(path: String): Boolean {
        val n = normalize(path)
        return n == "/login" || n == "/auth"
    }
}
