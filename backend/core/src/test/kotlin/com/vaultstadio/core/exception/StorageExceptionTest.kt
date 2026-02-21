/**
 * Tests for StorageException hierarchy (errorCode, httpStatus, message).
 * Exception types live in domain:common.
 */

package com.vaultstadio.core.exception

import com.vaultstadio.domain.common.exception.AuthenticationException
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.ConcurrentModificationException
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.FileSizeLimitException
import com.vaultstadio.domain.common.exception.InvalidOperationException
import com.vaultstadio.domain.common.exception.ItemAlreadyExistsException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.common.exception.PluginExecutionException
import com.vaultstadio.domain.common.exception.PluginLoadException
import com.vaultstadio.domain.common.exception.PluginNotFoundException
import com.vaultstadio.domain.common.exception.PluginVersionException
import com.vaultstadio.domain.common.exception.QuotaExceededException
import com.vaultstadio.domain.common.exception.ShareDownloadLimitException
import com.vaultstadio.domain.common.exception.ShareExpiredException
import com.vaultstadio.domain.common.exception.ShareNotFoundException
import com.vaultstadio.domain.common.exception.SharePasswordInvalidException
import com.vaultstadio.domain.common.exception.SharePasswordRequiredException
import com.vaultstadio.domain.common.exception.StorageBackendException
import com.vaultstadio.domain.common.exception.UserNotFoundException
import com.vaultstadio.domain.common.exception.ValidationException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class StorageExceptionTest {

    @Nested
    inner class ItemNotFoundExceptionTest {

        @Test
        fun `default message when no itemId or path`() {
            val e = ItemNotFoundException()
            assertEquals("ITEM_NOT_FOUND", e.errorCode)
            assertEquals(404, e.httpStatus)
            assertEquals("Item not found", e.message)
        }

        @Test
        fun `message includes itemId when provided`() {
            val e = ItemNotFoundException(itemId = "item-1")
            assertEquals("ITEM_NOT_FOUND", e.errorCode)
            assertEquals(404, e.httpStatus)
            assertNotNull(e.message)
            assertEquals("item-1", e.itemId)
        }

        @Test
        fun `message includes path when provided`() {
            val e = ItemNotFoundException(path = "/foo/bar")
            assertEquals("ITEM_NOT_FOUND", e.errorCode)
            assertEquals("Item not found at path: /foo/bar", e.message)
            assertEquals("/foo/bar", e.path)
        }
    }

    @Nested
    inner class UserNotFoundExceptionTest {

        @Test
        fun `default message and codes`() {
            val e = UserNotFoundException()
            assertEquals("USER_NOT_FOUND", e.errorCode)
            assertEquals(404, e.httpStatus)
            assertEquals("User not found", e.message)
        }

        @Test
        fun `stores userId and email`() {
            val e = UserNotFoundException(userId = "u1", email = "u@x.com")
            assertEquals("u1", e.userId)
            assertEquals("u@x.com", e.email)
        }
    }

    @Nested
    inner class ItemAlreadyExistsExceptionTest {

        @Test
        fun `message includes path`() {
            val e = ItemAlreadyExistsException(path = "/docs/file.txt")
            assertEquals("ITEM_ALREADY_EXISTS", e.errorCode)
            assertEquals(409, e.httpStatus)
            assertEquals("Item already exists at path: /docs/file.txt", e.message)
            assertEquals("/docs/file.txt", e.path)
        }
    }

    @Nested
    inner class ValidationExceptionTest {

        @Test
        fun `default message and codes`() {
            val e = ValidationException()
            assertEquals("VALIDATION_ERROR", e.errorCode)
            assertEquals(400, e.httpStatus)
        }

        @Test
        fun `message includes field when provided`() {
            val e = ValidationException(field = "email")
            assertEquals("Validation failed for field: email", e.message)
            assertEquals("email", e.field)
        }

        @Test
        fun `stores details map`() {
            val details = mapOf("min" to "0", "max" to "100")
            val e = ValidationException(details = details)
            assertEquals(details, e.details)
        }
    }

    @Nested
    inner class AuthenticationExceptionTest {

        @Test
        fun `default message and codes`() {
            val e = AuthenticationException()
            assertEquals("AUTHENTICATION_REQUIRED", e.errorCode)
            assertEquals(401, e.httpStatus)
            assertEquals("Authentication required", e.message)
        }

        @Test
        fun `custom message`() {
            val e = AuthenticationException("Invalid token")
            assertEquals("Invalid token", e.message)
        }
    }

    @Nested
    inner class AuthorizationExceptionTest {

        @Test
        fun `default message and codes`() {
            val e = AuthorizationException()
            assertEquals("ACCESS_DENIED", e.errorCode)
            assertEquals(403, e.httpStatus)
        }

        @Test
        fun `message includes requiredPermission when provided`() {
            val e = AuthorizationException(requiredPermission = "write")
            assertEquals("Access denied: requires write", e.message)
            assertEquals("write", e.requiredPermission)
        }
    }

    @Nested
    inner class QuotaExceededExceptionTest {

        @Test
        fun `message and codes`() {
            val e = QuotaExceededException(usedBytes = 1000, quotaBytes = 500, requiredBytes = 200)
            assertEquals("QUOTA_EXCEEDED", e.errorCode)
            assertEquals(507, e.httpStatus)
            assertEquals(1000, e.usedBytes)
            assertEquals(500, e.quotaBytes)
            assertEquals(200, e.requiredBytes)
        }
    }

    @Nested
    inner class FileSizeLimitExceptionTest {

        @Test
        fun `message and codes`() {
            val e = FileSizeLimitException(fileSize = 100_000_000, maxSize = 50_000_000)
            assertEquals("FILE_TOO_LARGE", e.errorCode)
            assertEquals(413, e.httpStatus)
            assertEquals(100_000_000, e.fileSize)
            assertEquals(50_000_000, e.maxSize)
        }
    }

    @Nested
    inner class InvalidOperationExceptionTest {

        @Test
        fun `message includes operation`() {
            val e = InvalidOperationException(operation = "delete_root")
            assertEquals("INVALID_OPERATION", e.errorCode)
            assertEquals(400, e.httpStatus)
            assertEquals("Invalid operation: delete_root", e.message)
            assertEquals("delete_root", e.operation)
        }
    }

    @Nested
    inner class StorageBackendExceptionTest {

        @Test
        fun `message and codes`() {
            val e = StorageBackendException(backend = "s3")
            assertEquals("STORAGE_BACKEND_ERROR", e.errorCode)
            assertEquals(500, e.httpStatus)
            assertEquals("s3", e.backend)
        }

        @Test
        fun `preserves cause`() {
            val cause = RuntimeException("io error")
            val e = StorageBackendException(backend = "local", cause = cause)
            assertEquals(cause, e.cause)
        }
    }

    @Nested
    inner class DatabaseExceptionTest {

        @Test
        fun `default message and codes`() {
            val e = DatabaseException()
            assertEquals("DATABASE_ERROR", e.errorCode)
            assertEquals(500, e.httpStatus)
            assertEquals("Database error", e.message)
        }

        @Test
        fun `custom message and cause`() {
            val cause = RuntimeException("connection lost")
            val e = DatabaseException("Connection failed", cause)
            assertEquals("Connection failed", e.message)
            assertEquals(cause, e.cause)
        }
    }

    @Nested
    inner class ConcurrentModificationExceptionTest {

        @Test
        fun `message and codes`() {
            val e = ConcurrentModificationException(
                itemId = "item-1",
                expectedVersion = 1,
                actualVersion = 2,
            )
            assertEquals("CONCURRENT_MODIFICATION", e.errorCode)
            assertEquals(409, e.httpStatus)
            assertEquals("item-1", e.itemId)
            assertEquals(1, e.expectedVersion)
            assertEquals(2, e.actualVersion)
        }
    }

    @Nested
    inner class ShareExceptionsTest {

        @Test
        fun `ShareNotFoundException`() {
            val e = ShareNotFoundException(shareId = "share-1")
            assertEquals("SHARE_NOT_FOUND", e.errorCode)
            assertEquals(404, e.httpStatus)
            assertEquals("share-1", e.shareId)
        }

        @Test
        fun `ShareExpiredException`() {
            val e = ShareExpiredException(shareId = "share-1")
            assertEquals("SHARE_EXPIRED", e.errorCode)
            assertEquals(410, e.httpStatus)
            assertEquals("Share has expired", e.message)
        }

        @Test
        fun `ShareDownloadLimitException`() {
            val e = ShareDownloadLimitException(shareId = "s1", downloadLimit = 10)
            assertEquals("SHARE_DOWNLOAD_LIMIT", e.errorCode)
            assertEquals(429, e.httpStatus)
            assertEquals(10, e.downloadLimit)
        }

        @Test
        fun `SharePasswordRequiredException`() {
            val e = SharePasswordRequiredException(shareId = "s1")
            assertEquals("SHARE_PASSWORD_REQUIRED", e.errorCode)
            assertEquals(401, e.httpStatus)
        }

        @Test
        fun `SharePasswordInvalidException`() {
            val e = SharePasswordInvalidException(shareId = "s1")
            assertEquals("SHARE_PASSWORD_INVALID", e.errorCode)
            assertEquals(403, e.httpStatus)
        }
    }

    @Nested
    inner class PluginExceptionsTest {

        @Test
        fun `PluginNotFoundException`() {
            val e = PluginNotFoundException(pluginId = "my-plugin")
            assertEquals("PLUGIN_NOT_FOUND", e.errorCode)
            assertEquals(404, e.httpStatus)
            assertEquals("my-plugin", e.pluginId)
        }

        @Test
        fun `PluginLoadException`() {
            val cause = RuntimeException("jar not found")
            val e = PluginLoadException(pluginId = "p1", cause = cause)
            assertEquals("PLUGIN_LOAD_ERROR", e.errorCode)
            assertEquals(500, e.httpStatus)
            assertEquals(cause, e.cause)
        }

        @Test
        fun `PluginExecutionException`() {
            val e = PluginExecutionException(pluginId = "p1", operation = "extract")
            assertEquals("PLUGIN_EXECUTION_ERROR", e.errorCode)
            assertEquals(500, e.httpStatus)
            assertEquals("extract", e.operation)
        }

        @Test
        fun `PluginVersionException`() {
            val e = PluginVersionException(
                pluginId = "p1",
                requiredVersion = "2.0",
                actualVersion = "1.0",
            )
            assertEquals("PLUGIN_VERSION_MISMATCH", e.errorCode)
            assertEquals(409, e.httpStatus)
            assertEquals("2.0", e.requiredVersion)
            assertEquals("1.0", e.actualVersion)
        }
    }
}
