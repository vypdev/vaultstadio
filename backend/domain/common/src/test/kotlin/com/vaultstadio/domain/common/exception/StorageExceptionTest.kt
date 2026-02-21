/**
 * Unit tests for [StorageException] hierarchy (error codes and HTTP status).
 */

package com.vaultstadio.domain.common.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StorageExceptionTest {

    @Test
    fun itemNotFoundExceptionHasCorrectErrorCodeAndStatus() {
        val e = ItemNotFoundException(itemId = "item-1")
        assertEquals("ITEM_NOT_FOUND", e.errorCode)
        assertEquals(404, e.httpStatus)
        assertEquals("Item not found: item-1", e.message)
    }

    @Test
    fun userNotFoundExceptionHasCorrectErrorCodeAndStatus() {
        val e = UserNotFoundException(userId = "user-1")
        assertEquals("USER_NOT_FOUND", e.errorCode)
        assertEquals(404, e.httpStatus)
    }

    @Test
    fun itemAlreadyExistsExceptionHasCorrectErrorCodeAndStatus() {
        val e = ItemAlreadyExistsException(path = "/foo/bar")
        assertEquals("ITEM_ALREADY_EXISTS", e.errorCode)
        assertEquals(409, e.httpStatus)
    }

    @Test
    fun validationExceptionHasCorrectErrorCodeAndStatus() {
        val e = ValidationException(field = "name", message = "Invalid")
        assertEquals("VALIDATION_ERROR", e.errorCode)
        assertEquals(400, e.httpStatus)
    }

    @Test
    fun authenticationExceptionHasCorrectErrorCodeAndStatus() {
        val e = AuthenticationException()
        assertEquals("AUTHENTICATION_REQUIRED", e.errorCode)
        assertEquals(401, e.httpStatus)
    }

    @Test
    fun authorizationExceptionHasCorrectErrorCodeAndStatus() {
        val e = AuthorizationException(requiredPermission = "admin")
        assertEquals("ACCESS_DENIED", e.errorCode)
        assertEquals(403, e.httpStatus)
    }

    @Test
    fun quotaExceededExceptionHasCorrectErrorCodeAndStatus() {
        val e = QuotaExceededException(usedBytes = 100, quotaBytes = 50, requiredBytes = 10)
        assertEquals("QUOTA_EXCEEDED", e.errorCode)
        assertEquals(507, e.httpStatus)
    }

    @Test
    fun fileSizeLimitExceptionHasCorrectErrorCodeAndStatus() {
        val e = FileSizeLimitException(fileSize = 1000, maxSize = 100)
        assertEquals("FILE_TOO_LARGE", e.errorCode)
        assertEquals(413, e.httpStatus)
    }

    @Test
    fun databaseExceptionHasCorrectErrorCodeAndStatus() {
        val e = DatabaseException("DB down")
        assertEquals("DATABASE_ERROR", e.errorCode)
        assertEquals(500, e.httpStatus)
    }

    @Test
    fun pluginNotFoundExceptionHasCorrectErrorCodeAndStatus() {
        val e = PluginNotFoundException(pluginId = "my-plugin")
        assertEquals("PLUGIN_NOT_FOUND", e.errorCode)
        assertEquals(404, e.httpStatus)
    }

    @Test
    fun shareNotFoundExceptionHasCorrectErrorCodeAndStatus() {
        val e = ShareNotFoundException(shareId = "s1")
        assertEquals("SHARE_NOT_FOUND", e.errorCode)
        assertEquals(404, e.httpStatus)
    }

    @Test
    fun shareExpiredExceptionHasCorrectErrorCodeAndStatus() {
        val e = ShareExpiredException(shareId = "s1")
        assertEquals("SHARE_EXPIRED", e.errorCode)
        assertEquals(410, e.httpStatus)
    }
}
