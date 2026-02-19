package com.vaultstadio.app.navigation

/**
 * Result of matching a URL path to a route (go_router style).
 *
 * [destination] is the matched screen; [pathParams] holds dynamic segments keyed by param name.
 * For the files tree, the "path" param holds the list of folder/file segments, e.g.
 * `/files/my_folder/other_folder/my-song.mp3` -> pathParams["path"] = ["my_folder", "other_folder", "my-song.mp3"].
 */
data class RouteMatch(
    val destination: MainDestination,
    /** Param name -> list of path segments (e.g. "path" -> ["folder", "file.mp3"]). */
    val pathParams: Map<String, List<String>> = emptyMap(),
) {
    /** Shortcut for the single splat param used by the files route. */
    fun pathSegments(): List<String> = pathParams["path"] ?: emptyList()
}
