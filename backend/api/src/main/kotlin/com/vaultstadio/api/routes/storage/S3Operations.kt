/**
 * S3 operations facade for route handlers.
 * Holds StorageService and MultipartUploadManagerInterface; delegates to S3Handlers.
 * Resolved via Koin so Routing does not inject services.
 */

package com.vaultstadio.api.routes.storage

import com.vaultstadio.core.domain.service.MultipartUploadManagerInterface
import com.vaultstadio.core.domain.service.StorageService
import io.ktor.server.application.ApplicationCall

class S3Operations(
    private val storageService: StorageService,
    private val multipartUploadManager: MultipartUploadManagerInterface,
) {

    suspend fun handleListBuckets(call: ApplicationCall, userId: String) {
        handleListBuckets(call, storageService, userId)
    }

    suspend fun handleListObjects(call: ApplicationCall, userId: String, bucket: String) {
        handleListObjects(call, storageService, userId, bucket)
    }

    suspend fun handleHeadBucket(call: ApplicationCall, userId: String?) {
        handleHeadBucket(call, userId)
    }

    suspend fun handleCreateBucket(call: ApplicationCall, bucket: String, userId: String?) {
        handleCreateBucket(call, storageService, bucket, userId)
    }

    suspend fun handleDeleteBucket(call: ApplicationCall, userId: String?) {
        handleDeleteBucket(call, userId)
    }

    suspend fun handleGetObject(call: ApplicationCall, userId: String, bucket: String, key: String) {
        handleGetObject(call, storageService, userId, bucket, key)
    }

    suspend fun handleHeadObject(call: ApplicationCall, userId: String, key: String) {
        handleHeadObject(call, storageService, userId, key)
    }

    suspend fun handlePutObject(
        call: ApplicationCall,
        userId: String?,
        bucket: String,
        key: String,
    ) {
        handlePutObject(
            call,
            storageService,
            multipartUploadManager,
            userId,
            bucket,
            key,
        )
    }

    suspend fun handleDeleteObject(call: ApplicationCall, userId: String?, key: String) {
        handleDeleteObject(
            call,
            storageService,
            multipartUploadManager,
            userId,
            key,
        )
    }

    suspend fun handleMultipartPost(
        call: ApplicationCall,
        userId: String?,
        bucket: String,
        key: String,
    ) {
        handleMultipartPost(
            call,
            storageService,
            multipartUploadManager,
            userId,
            bucket,
            key,
        )
    }
}
