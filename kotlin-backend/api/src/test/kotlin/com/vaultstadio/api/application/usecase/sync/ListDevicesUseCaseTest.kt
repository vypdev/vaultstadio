/**
 * ListDevicesUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.sync

import arrow.core.Either
import com.vaultstadio.core.domain.model.DeviceType
import com.vaultstadio.core.domain.model.SyncDevice
import com.vaultstadio.core.domain.service.SyncService
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ListDevicesUseCaseTest {

    private val syncService: SyncService = mockk()
    private val useCase = ListDevicesUseCaseImpl(syncService)

    @Test
    fun invokeReturnsDevicesWhenServiceSucceeds() = runTest {
        val now = Clock.System.now()
        val devices = listOf(
            SyncDevice(
                userId = "user-1",
                deviceId = "dev-1",
                deviceName = "My Laptop",
                deviceType = DeviceType.DESKTOP_MAC,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
        coEvery { syncService.listDevices("user-1", true) } returns Either.Right(devices)

        val result = useCase("user-1", activeOnly = true)

        assertTrue(result.isRight())
        val list = (result as Either.Right).value
        assertEquals(1, list.size)
        assertEquals("dev-1", list[0].deviceId)
        assertEquals("My Laptop", list[0].deviceName)
    }

    @Test
    fun invokeReturnsLeftWhenServiceFails() = runTest {
        coEvery { syncService.listDevices("user-1", false) } returns
            Either.Left(ItemNotFoundException(itemId = "x"))

        val result = useCase("user-1", activeOnly = false)

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }
}
