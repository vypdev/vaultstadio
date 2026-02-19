/**
 * RegisterDeviceUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.sync

import arrow.core.Either
import com.vaultstadio.core.domain.model.DeviceType
import com.vaultstadio.core.domain.model.SyncDevice
import com.vaultstadio.core.domain.service.RegisterDeviceInput
import com.vaultstadio.core.domain.service.SyncService
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegisterDeviceUseCaseTest {

    private val syncService: SyncService = mockk()
    private val useCase = RegisterDeviceUseCaseImpl(syncService)

    @Test
    fun invokeReturnsDeviceWhenServiceSucceeds() = runTest {
        val now = Clock.System.now()
        val input = RegisterDeviceInput(
            deviceId = "dev-1",
            deviceName = "My Laptop",
            deviceType = DeviceType.DESKTOP_MAC,
        )
        val device = SyncDevice(
            userId = "user-1",
            deviceId = "dev-1",
            deviceName = "My Laptop",
            deviceType = DeviceType.DESKTOP_MAC,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        coEvery { syncService.registerDevice(input, "user-1") } returns Either.Right(device)

        val result = useCase(input, "user-1")

        assertTrue(result.isRight())
        assertEquals("dev-1", (result as Either.Right).value.deviceId)
        assertEquals("My Laptop", (result).value.deviceName)
    }

    @Test
    fun invokeReturnsLeftWhenServiceFails() = runTest {
        val input = RegisterDeviceInput(
            deviceId = "dev-1",
            deviceName = "Laptop",
            deviceType = DeviceType.DESKTOP_LINUX,
        )
        coEvery { syncService.registerDevice(input, "user-1") } returns
            Either.Left(ItemNotFoundException(itemId = "x"))

        val result = useCase(input, "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }
}
