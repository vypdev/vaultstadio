/**
 * WebDAV operations facade for route handlers.
 * Holds StorageService and LockManager; delegates to WebDAVHandlers.
 * Resolved via Koin so Routing does not inject services.
 */

package com.vaultstadio.api.routes.storage

import com.vaultstadio.core.domain.service.LockManager
import com.vaultstadio.core.domain.service.StorageService
import io.ktor.server.application.ApplicationCall

// Alias to avoid recursion in handleOptions (same name as method)
private suspend fun runOptions(call: ApplicationCall) = handleOptions(call)

class WebDAVOperations(
    private val storageService: StorageService,
    private val lockManager: LockManager,
) {

    suspend fun handleOptions(call: ApplicationCall) {
        runOptions(call)
    }

    suspend fun handleGet(call: ApplicationCall, path: String, userId: String) {
        handleGet(call, path, storageService, userId)
    }

    suspend fun handlePut(call: ApplicationCall, path: String, userId: String) {
        handlePut(call, path, storageService, userId, lockManager)
    }

    suspend fun handleDelete(call: ApplicationCall, path: String, userId: String) {
        handleDelete(call, path, storageService, userId, lockManager)
    }

    suspend fun handleHead(call: ApplicationCall, path: String, userId: String) {
        handleHead(call, path, storageService, userId)
    }

    suspend fun handlePropfind(call: ApplicationCall, path: String, userId: String) {
        handlePropfind(call, path, storageService, userId, lockManager)
    }

    suspend fun handleProppatch(call: ApplicationCall, path: String, userId: String) {
        handleProppatch(call, path, storageService, userId)
    }

    suspend fun handleMkcol(call: ApplicationCall, path: String, userId: String) {
        handleMkcol(call, path, storageService, userId)
    }

    suspend fun handleCopy(call: ApplicationCall, path: String, userId: String) {
        handleCopy(call, path, storageService, userId)
    }

    suspend fun handleMove(call: ApplicationCall, path: String, userId: String) {
        handleMove(call, path, storageService, userId, lockManager)
    }

    suspend fun handleLock(call: ApplicationCall, path: String, userId: String) {
        handleLock(call, path, userId, lockManager)
    }

    suspend fun handleUnlock(call: ApplicationCall, path: String) {
        handleUnlock(call, path, lockManager)
    }
}
