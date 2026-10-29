package com.vaultstadio.api.routes.share

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.vaultstadio.application.usecase.share.DownloadPublicShareUseCase
import com.vaultstadio.domain.share.access.PublicShareFailure
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.parseQueryString
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining
import org.koin.ktor.ext.get as koinGet

internal fun Route.publicSharePageRoutes() {
    get("/share-assets/public-share.mjs") { call.asset("public-share.mjs", ContentType.Application.JavaScript) }
    get("/share-assets/public-share.css") { call.asset("public-share.css", ContentType.Text.CSS) }
    get("/share/{token}/view") {
        call.publicShareHeaders(page = true)
        if (call.rejectPasswordQuery()) return@get
        call.sharePage()
    }
    post("/share/{token}/view/download") {
        call.publicShareHeaders(page = true)
        if (call.rejectPasswordQuery()) return@post
        call.formPassword().fold(
            { call.sharePage(it, downloadPage = true) },
            { password ->
                val download: DownloadPublicShareUseCase = call.application.koinGet()
                download(call.shareInput(password)).fold({
                    call.sharePage(it, downloadPage = true)
                }, { call.streamShare(it) })
            },
        )
    }
}

private suspend fun ApplicationCall.formPassword(): Either<PublicShareFailure, String?> = either {
    ensure(request.contentType().match(ContentType.Application.FormUrlEncoded)) { PublicShareFailure.INVALID_REQUEST }
    val body = receiveChannel().readRemaining(MAX_FORM_BYTES + 1).readText()
    ensure(body.toByteArray().size <= MAX_FORM_BYTES) { PublicShareFailure.INVALID_REQUEST }
    val fields = try {
        parseQueryString(body)
    } catch (_: IllegalArgumentException) {
        raise(PublicShareFailure.INVALID_REQUEST)
    }
    val passwordFields = fields.getAll("password").orEmpty()
    ensure(passwordFields.size <= 1) { PublicShareFailure.INVALID_REQUEST }
    passwordFields.singleOrNull()?.takeIf { it.isNotEmpty() }
}

private suspend fun ApplicationCall.sharePage(failure: PublicShareFailure? = null, downloadPage: Boolean = false) {
    // Keep the form's Origin while withholding the capability path from Referer.
    publicShareHeaders(page = true)
    response.header(
        "Content-Security-Policy",
        "default-src 'none'; script-src 'self'; style-src 'self'; connect-src 'self'; " +
            "form-action 'self'; base-uri 'none'; frame-ancestors 'none'",
    )
    val html = resource("public-share.html")
        .replace("{{ERROR}}", failure?.description.orEmpty())
        .replace("{{ASSET_PREFIX}}", if (downloadPage) "../../../share-assets" else "../../share-assets")
    respondText(html, ContentType.Text.Html, failure?.httpStatus ?: HttpStatusCode.OK)
}

private suspend fun ApplicationCall.asset(name: String, type: ContentType) {
    publicShareHeaders()
    respondText(resource(name), type)
}

private fun resource(name: String): String = checkNotNull(
    object {}.javaClass.getResource("/public-share/$name"),
).readText()

private const val MAX_FORM_BYTES = 2048L
