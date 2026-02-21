/**
 * VaultStadio S3-Compatible API Routes
 *
 * Implements a subset of the AWS S3 API for compatibility with S3 clients and tools.
 * Supports: ListBuckets, ListObjects, GetObject, PutObject, DeleteObject, HeadObject, CopyObject, MultipartUpload.
 *
 * Authentication:
 * - Uses "s3-signature" authentication provider (AWS Signature V4)
 * - Falls back to JWT authentication if S3 signature provider not configured
 *
 * Usage with AWS CLI:
 * ```bash
 * aws configure
 * # Access Key: Your VaultStadio API key
 * # Secret Key: Your VaultStadio API secret
 * # Region: us-east-1 (any value works)
 * aws s3 ls --endpoint-url https://server/s3
 * ```
 *
 * @see docs/PHASE6_ADVANCED_FEATURES.md for full configuration details
 */

package com.vaultstadio.api.routes.storage

import com.vaultstadio.api.config.user
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.util.UUID
import org.koin.ktor.ext.get as koinGet

/**
 * S3 error codes following AWS S3 specification.
 */
object S3ErrorCodes {
    const val ACCESS_DENIED = "AccessDenied"
    const val BUCKET_NOT_EMPTY = "BucketNotEmpty"
    const val BUCKET_ALREADY_EXISTS = "BucketAlreadyExists"
    const val INVALID_BUCKET_NAME = "InvalidBucketName"
    const val INVALID_ACCESS_KEY_ID = "InvalidAccessKeyId"
    const val NO_SUCH_BUCKET = "NoSuchBucket"
    const val NO_SUCH_KEY = "NoSuchKey"
    const val NO_SUCH_UPLOAD = "NoSuchUpload"
    const val SIGNATURE_DOES_NOT_MATCH = "SignatureDoesNotMatch"
    const val INTERNAL_ERROR = "InternalError"
    const val INVALID_PART = "InvalidPart"
    const val INVALID_PART_ORDER = "InvalidPartOrder"
}

/**
 * Build S3 error response XML following AWS S3 format.
 */
fun buildS3Error(
    code: String,
    message: String,
    resource: String? = null,
    requestId: String = UUID.randomUUID().toString(),
): String {
    return buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<Error>""")
        appendLine("""  <Code>$code</Code>""")
        appendLine("""  <Message>$message</Message>""")
        resource?.let { appendLine("""  <Resource>$it</Resource>""") }
        appendLine("""  <RequestId>$requestId</RequestId>""")
        appendLine("""</Error>""")
    }
}

/**
 * Get the authenticated user ID from session principal (User).
 */
private fun ApplicationCall.getUserId(): String? = user?.id

/**
 * Configure S3-compatible routes.
 * Resolves S3Operations via Koin; no service injection in Routing.
 */
fun Route.s3Routes() {
    authenticate("s3-signature", "jwt", optional = true) {
        route("/s3") {
            get {
                val userId = call.getUserId() ?: "anonymous"
                call.application.koinGet<S3Operations>().handleListBuckets(call, userId)
            }

            route("/{bucket}") {
                get {
                    val bucket = call.parameters["bucket"]!!
                    val userId = call.getUserId() ?: "anonymous"
                    call.application.koinGet<S3Operations>().handleListObjects(call, userId, bucket)
                }
                head {
                    call.application.koinGet<S3Operations>().handleHeadBucket(call, call.getUserId())
                }
                put {
                    val bucket = call.parameters["bucket"]!!
                    call.application.koinGet<S3Operations>().handleCreateBucket(
                        call,
                        bucket,
                        call.getUserId(),
                    )
                }
                delete {
                    call.application.koinGet<S3Operations>().handleDeleteBucket(call, call.getUserId())
                }

                route("/{key...}") {
                    get {
                        val key = call.parameters.getAll("key")?.joinToString("/") ?: ""
                        val bucket = call.parameters["bucket"]!!
                        val userId = call.getUserId() ?: "anonymous"
                        call.application.koinGet<S3Operations>().handleGetObject(
                            call,
                            userId,
                            bucket,
                            key,
                        )
                    }
                    head {
                        val key = call.parameters.getAll("key")?.joinToString("/") ?: ""
                        val userId = call.getUserId() ?: "anonymous"
                        call.application.koinGet<S3Operations>().handleHeadObject(call, userId, key)
                    }
                    put {
                        val key = call.parameters.getAll("key")?.joinToString("/") ?: ""
                        val bucket = call.parameters["bucket"]!!
                        call.application.koinGet<S3Operations>().handlePutObject(
                            call,
                            call.getUserId(),
                            bucket,
                            key,
                        )
                    }
                    delete {
                        val key = call.parameters.getAll("key")?.joinToString("/") ?: ""
                        call.application.koinGet<S3Operations>().handleDeleteObject(
                            call,
                            call.getUserId(),
                            key,
                        )
                    }
                }
            }

            route("/{bucket}/{key...}") {
                post {
                    val bucket = call.parameters["bucket"]!!
                    val key = call.parameters.getAll("key")?.joinToString("/") ?: ""
                    call.application.koinGet<S3Operations>().handleMultipartPost(
                        call,
                        call.getUserId(),
                        bucket,
                        key,
                    )
                }
            }
        }
    }
}
