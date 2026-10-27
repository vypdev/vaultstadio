package com.vaultstadio.api.routes.metadata

import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.searchRoutes() {
    route("/search") {
        basicSearchRoutes()
        advancedSearchRoutes()
        metadataSearchRoutes()
        suggestionRoutes()
    }
}
