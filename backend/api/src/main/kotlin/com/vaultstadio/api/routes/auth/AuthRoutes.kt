/**
 * VaultStadio Authentication Routes
 */

package com.vaultstadio.api.routes.auth

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.LoginRequest
import com.vaultstadio.api.dto.LoginResponse
import com.vaultstadio.api.dto.RefreshRequest
import com.vaultstadio.api.dto.RefreshResponse
import com.vaultstadio.api.dto.RegisterRequest
import com.vaultstadio.api.dto.toResponse
import com.vaultstadio.api.application.usecase.auth.LoginUseCase
import com.vaultstadio.api.application.usecase.auth.LogoutUseCase
import com.vaultstadio.api.application.usecase.auth.RefreshSessionUseCase
import com.vaultstadio.api.application.usecase.auth.RegisterUseCase
import com.vaultstadio.core.domain.service.LoginInput
import com.vaultstadio.core.domain.service.RegisterUserInput
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.get as koinGet

fun Route.authRoutes() {
    route("/auth") {
        post("/register") {
            val registerUseCase: RegisterUseCase = call.application.koinGet()
            val request = call.receive<RegisterRequest>()

            val input = RegisterUserInput(
                email = request.email,
                username = request.username,
                password = request.password,
            )

            registerUseCase(input).fold(
                ifLeft = { throw it },
                ifRight = { user ->
                    call.respond(
                        HttpStatusCode.Created,
                        ApiResponse(
                            success = true,
                            data = user.toResponse(),
                        ),
                    )
                },
            )
        }

        post("/login") {
            val loginUseCase: LoginUseCase = call.application.koinGet()
            val request = call.receive<LoginRequest>()

            val input = LoginInput(
                email = request.email,
                password = request.password,
                ipAddress = call.request.local.remoteAddress,
                userAgent = call.request.headers["User-Agent"],
            )

            loginUseCase(input).fold(
                { error -> throw error },
                { result ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = LoginResponse(
                                user = result.user.toResponse(),
                                token = result.sessionToken,
                                refreshToken = result.refreshToken,
                                expiresAt = result.expiresAt,
                            ),
                        ),
                    )
                },
            )
        }

        post("/refresh") {
            val refreshSessionUseCase: RefreshSessionUseCase = call.application.koinGet()
            val request = call.receive<RefreshRequest>()

            refreshSessionUseCase(request.refreshToken).fold(
                { error -> throw error },
                { result ->
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = RefreshResponse(
                                user = result.user.toResponse(),
                                token = result.sessionToken,
                                refreshToken = result.refreshToken,
                                expiresAt = result.expiresAt,
                            ),
                        ),
                    )
                },
            )
        }

        post("/logout") {
            val logoutUseCase: LogoutUseCase = call.application.koinGet()
            val token = call.request.headers["Authorization"]
                ?.removePrefix("Bearer ")

            if (token != null) {
                logoutUseCase(token)
            }

            call.respond(
                HttpStatusCode.OK,
                ApiResponse<Unit>(success = true),
            )
        }

        // Authenticated endpoint to get current user
        authenticate("jwt") {
            get("/me") {
                val user = call.user!!
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = user.toResponse(),
                    ),
                )
            }
        }
    }
}
