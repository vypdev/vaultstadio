package com.vaultstadio.app.navigation

/**
 * URL path helpers backed by [AppRoutes].
 * Routes are declared per-screen in [AppRoute] with path and parent (go_router style).
 */
object RoutePaths {

    /** Path shown when the user is on the login/auth screen. */
    const val AUTH_PATH: String = AppRoutes.AUTH_PATH

    /** Default path for the main area (files list). */
    const val DEFAULT_MAIN_PATH: String = AppRoutes.DEFAULT_MAIN_PATH

    /** Whether the path is the auth (login) screen. */
    fun isAuthPath(path: String): Boolean = AppRoutes.isAuthPath(path)

    /** Canonical URL path for a destination, optionally with path params (e.g. file path segments). */
    fun toPath(destination: MainDestination, pathParams: Map<String, List<String>>? = null): String =
        AppRoutes.toPath(destination, pathParams)

    /** Destination for a path, or null if not recognized. */
    fun fromPath(path: String): MainDestination? = AppRoutes.fromPath(path)

    /** Matches path and returns destination plus path params (go_router style). */
    fun parseRoute(path: String): RouteMatch? = AppRoutes.parseRoute(path)

    /** Navigation stack for a path (root first, then ancestors, then leaf). */
    fun getStack(path: String): List<MainDestination> = AppRoutes.getStack(path)
}
