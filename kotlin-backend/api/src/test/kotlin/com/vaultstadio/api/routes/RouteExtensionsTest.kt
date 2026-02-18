/**
 * VaultStadio Route Extensions Tests
 *
 * Unit tests for respondEither, respondApiEither, and respondEitherUnit:
 * Right path completes without throwing; Left path throws the exception.
 */

package com.vaultstadio.api.routes

import arrow.core.Either
import com.vaultstadio.core.exception.ItemNotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RouteExtensionsTest {

    private val call = mockk<ApplicationCall>(relaxed = true)

    @Test
    fun `respondEither with Right completes without throwing`() = runTest {
        call.respondEither(Either.Right(42), HttpStatusCode.OK)
    }

    @Test
    fun `respondEither with Left throws StorageException`() = runTest {
        val error = ItemNotFoundException(itemId = "x")
        assertThrows<ItemNotFoundException> {
            call.respondEither(Either.Left(error))
        }
    }

    @Test
    fun `respondApiEither with Right completes without throwing`() = runTest {
        call.respondApiEither(Either.Right(100), HttpStatusCode.Created)
    }

    @Test
    fun `respondApiEither with Left throws StorageException`() = runTest {
        val error = ItemNotFoundException(path = "/y")
        assertThrows<ItemNotFoundException> {
            call.respondApiEither(Either.Left(error))
        }
    }

    @Test
    fun `respondEitherUnit with Right completes without throwing`() = runTest {
        call.respondEitherUnit(Either.Right(Unit), HttpStatusCode.NoContent)
    }

    @Test
    fun `respondEitherUnit with Left throws StorageException`() = runTest {
        val error = ItemNotFoundException(itemId = "z")
        assertThrows<ItemNotFoundException> {
            call.respondEitherUnit(Either.Left(error))
        }
    }
}
