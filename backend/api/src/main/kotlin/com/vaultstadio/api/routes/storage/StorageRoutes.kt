package com.vaultstadio.api.routes.storage

import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.storageRoutes() {
    route("/storage") {
        storageListingRoutes()
        readsStorageRoutes()
        mutationsStorageRoutes()
        transfersStorageRoutes()
        trashStorageRoutes()
    }
}
