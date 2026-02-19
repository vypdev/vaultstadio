/**
 * VaultStadio Swagger/OpenAPI Configuration
 *
 * Configures Swagger UI for API documentation.
 */

package com.vaultstadio.api.config

import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing

/**
 * Configures Swagger UI and OpenAPI documentation.
 */
fun Application.configureSwagger() {
    routing {
        // Swagger UI endpoint
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml") {
            version = "5.10.3"
        }
    }
}
