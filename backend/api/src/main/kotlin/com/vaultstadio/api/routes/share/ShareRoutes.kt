package com.vaultstadio.api.routes.share

import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.shareRoutes() {
    route("/shares") {
        ownerShareListRoutes()
        ownerItemShareRoute()
        ownerShareCreateRoute()
        ownerShareDeleteRoute()
    }
}
