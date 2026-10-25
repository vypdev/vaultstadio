/**
 * VaultStadio Security Configuration
 */

package com.vaultstadio.api.config

import com.vaultstadio.core.domain.service.LoginInput
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.auth.model.User
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.Principal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.auth.bearer
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import org.koin.ktor.ext.get

/**
 * Principal containing the authenticated user.
 */
data class UserPrincipal(val user: User) : Principal

/**
 * Extension to get the current authenticated user.
 */
val ApplicationCall.user: User?
    get() = principal<UserPrincipal>()?.user

fun Application.configureSecurity() {
    val userService: UserService = get()

    install(Authentication) {
        // Bearer token authentication
        bearer("auth-bearer") {
            realm = "VaultStadio"
            authenticate { tokenCredential ->
                userService.validateSession(tokenCredential.token).fold(
                    { null },
                    { user -> UserPrincipal(user) },
                )
            }
        }

        // Optional authentication (allows unauthenticated access)
        bearer("auth-optional") {
            realm = "VaultStadio"
            authenticate { tokenCredential ->
                userService.validateSession(tokenCredential.token).fold(
                    { null },
                    { user -> UserPrincipal(user) },
                )
            }
            skipWhen { call ->
                call.request.headers[HttpHeaders.Authorization].isNullOrEmpty()
            }
        }

        // JWT (alias for auth-bearer) – used by AuthRoutes, S3 and WebDAV fallback
        bearer("jwt") {
            realm = "VaultStadio"
            authenticate { tokenCredential ->
                userService.validateSession(tokenCredential.token).fold(
                    { null },
                    { user -> UserPrincipal(user) },
                )
            }
        }

        // WebDAV Basic Auth – validate email/password via UserService
        basic("webdav-basic") {
            realm = "VaultStadio"
            validate { credentials ->
                userService.login(
                    LoginInput(
                        email = credentials.name,
                        password = credentials.password,
                        ipAddress = null,
                        userAgent = null,
                    ),
                ).fold(
                    { null },
                    { result -> UserPrincipal(result.user) },
                )
            }
        }
    }
}

/**
 * Responds with 401 if not authenticated.
 */
suspend fun ApplicationCall.requireAuth(): User {
    return user ?: run {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
        throw AuthenticationException()
    }
}

class AuthenticationException : Exception("Authentication required")
