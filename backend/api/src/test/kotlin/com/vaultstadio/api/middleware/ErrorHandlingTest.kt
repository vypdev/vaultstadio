/**
 * VaultStadio Error Handling Middleware Tests
 */

package com.vaultstadio.api.middleware

import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.core.exception.DatabaseException
import com.vaultstadio.core.exception.ItemNotFoundException
import com.vaultstadio.core.exception.StorageBackendException
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for Error Handling middleware components.
 */
class ErrorHandlingTest {

    @Nested
    @DisplayName("ApiError Tests")
    inner class ApiErrorTests {

        @Test
        fun `ApiError should have code and message`() {
            val error = ApiError(
                code = "NOT_FOUND",
                message = "Resource not found",
            )

            assertEquals("NOT_FOUND", error.code)
            assertEquals("Resource not found", error.message)
        }

        @Test
        fun `ApiError should support various error codes`() {
            val errorCodes = listOf(
                "NOT_FOUND",
                "UNAUTHORIZED",
                "FORBIDDEN",
                "VALIDATION_ERROR",
                "INTERNAL_ERROR",
                "AUTHENTICATION_REQUIRED",
            )

            errorCodes.forEach { code ->
                val error = ApiError(code = code, message = "Error message")
                assertEquals(code, error.code)
            }
        }
    }

    @Nested
    @DisplayName("ApiResponse Tests")
    inner class ApiResponseTests {

        @Test
        fun `successful response should have success true and data`() {
            val response = ApiResponse(
                success = true,
                data = "test data",
                error = null,
            )

            assertTrue(response.success)
            assertEquals("test data", response.data)
            assertEquals(null, response.error)
        }

        @Test
        fun `error response should have success false and error`() {
            val response = ApiResponse<String>(
                success = false,
                data = null,
                error = ApiError("ERROR", "Something went wrong"),
            )

            assertFalse(response.success)
            assertEquals(null, response.data)
            assertNotNull(response.error)
            assertEquals("ERROR", response.error?.code)
        }
    }

    @Nested
    @DisplayName("StorageException Tests")
    inner class StorageExceptionTests {

        @Test
        fun `ItemNotFoundException should have correct HTTP status`() {
            val exception = ItemNotFoundException("item-123")

            assertEquals(404, exception.httpStatus)
            assertNotNull(exception.errorCode)
        }

        @Test
        fun `DatabaseException should have correct HTTP status`() {
            val exception = DatabaseException("Database error")

            assertEquals(500, exception.httpStatus)
            assertNotNull(exception.errorCode)
        }

        @Test
        fun `StorageBackendException should have correct HTTP status`() {
            val exception = StorageBackendException("backend", "Storage error")

            assertEquals(500, exception.httpStatus)
            assertNotNull(exception.errorCode)
        }
    }

    @Nested
    @DisplayName("HTTP Status Code Mapping Tests")
    inner class StatusCodeMappingTests {

        @Test
        fun `400 Bad Request should be created correctly`() {
            val status = HttpStatusCode.BadRequest

            assertEquals(400, status.value)
            assertEquals("Bad Request", status.description)
        }

        @Test
        fun `401 Unauthorized should be created correctly`() {
            val status = HttpStatusCode.Unauthorized

            assertEquals(401, status.value)
            assertEquals("Unauthorized", status.description)
        }

        @Test
        fun `403 Forbidden should be created correctly`() {
            val status = HttpStatusCode.Forbidden

            assertEquals(403, status.value)
            assertEquals("Forbidden", status.description)
        }

        @Test
        fun `404 Not Found should be created correctly`() {
            val status = HttpStatusCode.NotFound

            assertEquals(404, status.value)
            assertEquals("Not Found", status.description)
        }

        @Test
        fun `500 Internal Server Error should be created correctly`() {
            val status = HttpStatusCode.InternalServerError

            assertEquals(500, status.value)
            assertEquals("Internal Server Error", status.description)
        }

        @Test
        fun `status code can be created from value`() {
            val status = HttpStatusCode.fromValue(404)

            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Nested
    @DisplayName("Exception Error Code Tests")
    inner class ExceptionErrorCodeTests {

        @Test
        fun `exceptions should have meaningful error codes`() {
            val itemNotFound = ItemNotFoundException("item-123")
            val dbError = DatabaseException("DB error")
            val backendError = StorageBackendException("s3", "S3 error")

            assertNotNull(itemNotFound.errorCode)
            assertNotNull(dbError.errorCode)
            assertNotNull(backendError.errorCode)

            // Error codes should be non-empty strings
            assertTrue(itemNotFound.errorCode.isNotEmpty())
            assertTrue(dbError.errorCode.isNotEmpty())
            assertTrue(backendError.errorCode.isNotEmpty())
        }
    }

    @Nested
    @DisplayName("Error Response Format Tests")
    inner class ErrorResponseFormatTests {

        @Test
        fun `error response should follow consistent format`() {
            val error = ApiError(
                code = "VALIDATION_ERROR",
                message = "Invalid input data",
            )

            val response = ApiResponse<Unit>(
                success = false,
                error = error,
            )

            assertFalse(response.success)
            assertNotNull(response.error)
            assertEquals("VALIDATION_ERROR", response.error?.code)
            assertEquals("Invalid input data", response.error?.message)
        }

        @Test
        fun `success response should not have error`() {
            val response = ApiResponse(
                success = true,
                data = mapOf("id" to "123", "name" to "test"),
                error = null,
            )

            assertTrue(response.success)
            assertNotNull(response.data)
            assertEquals(null, response.error)
        }
    }
}
