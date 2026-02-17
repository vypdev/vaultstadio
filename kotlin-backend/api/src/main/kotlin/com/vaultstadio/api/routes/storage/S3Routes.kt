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
import com.vaultstadio.core.domain.service.MultipartUploadManagerInterface
import com.vaultstadio.core.domain.service.StorageService
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
 *
 * @param storageService Service for file operations
 * @param multipartUploadManager Manager for multipart uploads
 */
fun Route.s3Routes(storageService: StorageService, multipartUploadManager: MultipartUploadManagerInterface) {
    authenticate("s3-signature", "jwt", optional = true) {
        route("/s3") {
            get {
                val userId = call.getUserId() ?: "anonymous"
                handleListBuckets(call, storageService, userId)
            }

            route("/{bucket}") {
                get {
                    val bucket = call.parameters["bucket"]!!
                    val userId = call.getUserId() ?: "anonymous"
                    handleListObjects(call, storageService, userId, bucket)
                }
                head {
                    handleHeadBucket(call, call.getUserId())
                }
                put {
                    val bucket = call.parameters["bucket"]!!
                    handleCreateBucket(call, storageService, bucket, call.getUserId())
                }
                delete {
                    handleDeleteBucket(call, call.getUserId())
                }

                route("/{key...}") {
                    get {
                        val key = call.parameters.getAll("key")?.joinToString("/") ?: ""
                        val bucket = call.parameters["bucket"]!!
                        val userId = call.getUserId() ?: "anonymous"
                        handleGetObject(call, storageService, userId, bucket, key)
                    }
                    head {
                        val key = call.parameters.getAll("key")?.joinToString("/") ?: ""
                        val userId = call.getUserId() ?: "anonymous"
                        handleHeadObject(call, storageService, userId, key)
                    }
                    put {
                        val key = call.parameters.getAll("key")?.joinToString("/") ?: ""
                        val bucket = call.parameters["bucket"]!!
                        handlePutObject(
                            call,
                            storageService,
                            multipartUploadManager,
                            call.getUserId(),
                            bucket,
                            key,
                        )
                    }
                    delete {
                        val key = call.parameters.getAll("key")?.joinToString("/") ?: ""
                        handleDeleteObject(
                            call,
                            storageService,
                            multipartUploadManager,
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
                    handleMultipartPost(
                        call,
                        storageService,
                        multipartUploadManager,
                        call.getUserId(),
                        bucket,
                        key,
                    )
                }
            }
        }
    }
}
