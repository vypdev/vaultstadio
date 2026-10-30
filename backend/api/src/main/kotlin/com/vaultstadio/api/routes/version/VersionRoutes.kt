package com.vaultstadio.api.routes.version

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.versionRoutes() {
    authenticate("auth-bearer") {
        route("/api/v1/versions") {
            versionHistoryRoutes()
            versionDetailRoutes()
            versionMutationRoutes()
        }
    }
}
