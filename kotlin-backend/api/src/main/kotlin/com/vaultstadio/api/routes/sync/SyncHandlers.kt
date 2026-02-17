/**
 * Sync request handlers and response mappers.
 * Extracted from SyncRoutes to keep the main file under the line limit.
 */

package com.vaultstadio.api.routes.sync

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.core.domain.model.ChangeType
import com.vaultstadio.core.domain.model.ConflictResolution
import com.vaultstadio.core.domain.model.DeviceType
import com.vaultstadio.core.domain.model.SyncChange
import com.vaultstadio.core.domain.model.SyncConflict
import com.vaultstadio.core.domain.model.SyncDevice
import com.vaultstadio.core.domain.service.RecordChangeInput
import com.vaultstadio.core.domain.service.RegisterDeviceInput
import com.vaultstadio.core.domain.service.SyncService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import kotlinx.datetime.Clock
import com.vaultstadio.core.domain.model.SyncRequest as CoreSyncRequest

internal suspend fun handleRegisterDevice(call: ApplicationCall, syncService: SyncService) {
    val request = call.receive<RegisterDeviceRequest>()
    val user = call.user ?: run {
        call.respond(HttpStatusCode.Unauthorized)
        return
    }
    val deviceType = try {
        DeviceType.valueOf(request.deviceType)
    } catch (e: Exception) {
        DeviceType.OTHER
    }
    val input = RegisterDeviceInput(
        deviceId = request.deviceId,
        deviceName = request.deviceName,
        deviceType = deviceType,
    )
    syncService.registerDevice(input, user.id).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { device ->
            call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = device.toResponse()))
        },
    )
}

internal suspend fun handleListDevices(call: ApplicationCall, syncService: SyncService) {
    val activeOnly = call.request.queryParameters["activeOnly"]?.toBoolean() ?: true
    val user = call.user ?: run {
        call.respond(HttpStatusCode.Unauthorized)
        return
    }
    syncService.listDevices(user.id, activeOnly).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { devices ->
            call.respond(ApiResponse(success = true, data = devices.map { it.toResponse() }))
        },
    )
}

internal suspend fun handleDeactivateDevice(call: ApplicationCall, syncService: SyncService) {
    val deviceId = call.parameters["deviceId"]
        ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing device ID")
            return
        }
    val user = call.user ?: run {
        call.respond(HttpStatusCode.Unauthorized)
        return
    }
    syncService.deactivateDevice(deviceId, user.id).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { call.respond(HttpStatusCode.OK, mapOf("message" to "Device deactivated")) },
    )
}

internal suspend fun handleRemoveDevice(call: ApplicationCall, syncService: SyncService) {
    val deviceId = call.parameters["deviceId"]
        ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing device ID")
            return
        }
    val user = call.user ?: run {
        call.respond(HttpStatusCode.Unauthorized)
        return
    }
    syncService.removeDevice(deviceId, user.id).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { call.respond(HttpStatusCode.NoContent) },
    )
}

internal suspend fun handlePull(call: ApplicationCall, syncService: SyncService) {
    val deviceId = call.request.headers["X-Device-ID"]
        ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing X-Device-ID header")
            return
        }
    val request = call.receive<SyncRequest>()
    val user = call.user ?: run {
        call.respond(HttpStatusCode.Unauthorized)
        return
    }
    val syncRequest = CoreSyncRequest(
        deviceId = deviceId,
        cursor = request.cursor,
        limit = request.limit,
    )
    syncService.sync(syncRequest, user.id).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { response ->
            call.respond(
                ApiResponse(
                    success = true,
                    data = SyncResponse(
                        changes = response.changes.map { it.toResponse() },
                        cursor = response.cursor,
                        hasMore = response.hasMore,
                        conflicts = response.conflicts.map { it.toResponse() },
                        serverTime = Clock.System.now().toString(),
                    ),
                ),
            )
        },
    )
}

internal suspend fun handlePush(call: ApplicationCall, syncService: SyncService) {
    val deviceId = call.request.headers["X-Device-ID"]
        ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing X-Device-ID header")
            return
        }
    val request = call.receive<PushChangesRequest>()
    val user = call.user ?: run {
        call.respond(HttpStatusCode.Unauthorized)
        return
    }
    var accepted = 0
    val conflicts = mutableListOf<SyncConflict>()
    for (change in request.changes) {
        val changeType = try {
            ChangeType.valueOf(change.changeType)
        } catch (e: Exception) {
            ChangeType.MODIFY
        }
        val input = RecordChangeInput(
            itemId = change.itemId,
            changeType = changeType,
            deviceId = deviceId,
            oldPath = change.oldPath,
            newPath = change.newPath,
            checksum = change.checksum,
            metadata = change.metadata,
        )
        syncService.recordChange(input, user.id).fold(
            { _ -> },
            { _ -> accepted++ },
        )
    }
    call.respond(
        mapOf(
            "accepted" to accepted,
            "conflicts" to conflicts.map { it.toResponse() },
        ),
    )
}

internal suspend fun handleGetPendingConflicts(call: ApplicationCall, syncService: SyncService) {
    val user = call.user ?: run {
        call.respond(HttpStatusCode.Unauthorized)
        return
    }
    syncService.getPendingConflicts(user.id).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { conflicts ->
            call.respond(ApiResponse(success = true, data = conflicts.map { it.toResponse() }))
        },
    )
}

internal suspend fun handleResolveConflict(call: ApplicationCall, syncService: SyncService) {
    val conflictId = call.parameters["conflictId"]
        ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing conflict ID")
            return
        }
    val request = call.receive<ResolveConflictRequest>()
    val user = call.user ?: run {
        call.respond(HttpStatusCode.Unauthorized)
        return
    }
    val resolution = try {
        ConflictResolution.valueOf(request.resolution)
    } catch (e: Exception) {
        call.respond(HttpStatusCode.BadRequest, "Invalid resolution")
        return
    }
    syncService.resolveConflict(conflictId, resolution, user.id).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { call.respond(HttpStatusCode.OK, mapOf("message" to "Conflict resolved")) },
    )
}

internal suspend fun handleGetFileSignature(call: ApplicationCall, syncService: SyncService) {
    val itemId = call.parameters["itemId"]
        ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing item ID")
            return
        }
    val blockSize = call.request.queryParameters["blockSize"]?.toIntOrNull() ?: 4096
    val versionNumber = call.request.queryParameters["version"]?.toIntOrNull() ?: 1
    syncService.generateFileSignature(itemId, versionNumber, blockSize).fold(
        { error -> call.respond(HttpStatusCode.NotFound, error.message ?: "Error") },
        { signature ->
            call.respond(
                FileSignatureResponse(
                    itemId = signature.itemId,
                    blockSize = signature.blockSize,
                    versionNumber = signature.versionNumber,
                    blocks = signature.blocks.map { block ->
                        BlockSignatureResponse(
                            index = block.index,
                            weakChecksum = block.weakChecksum,
                            strongChecksum = block.strongChecksum,
                        )
                    },
                ),
            )
        },
    )
}

internal suspend fun handleDeltaUpload(call: ApplicationCall, syncService: SyncService) {
    val itemId = call.parameters["itemId"]
        ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing item ID")
            return
        }
    val user = call.user ?: run {
        call.respond(HttpStatusCode.Unauthorized)
        return
    }
    val request = call.receive<DeltaUploadRequest>()
    if (request.blocks.isEmpty()) {
        call.respond(HttpStatusCode.BadRequest, "No delta blocks provided")
        return
    }
    val changeInput = RecordChangeInput(
        itemId = itemId,
        changeType = ChangeType.MODIFY,
        deviceId = call.request.headers["X-Device-ID"],
        checksum = request.newChecksum,
    )
    syncService.recordChange(changeInput, user.id).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { change ->
            call.respond(
                HttpStatusCode.OK,
                DeltaUploadResponse(
                    success = true,
                    itemId = itemId,
                    appliedBlocks = request.blocks.size,
                    newVersion = change.cursor.toInt(),
                    newChecksum = request.newChecksum ?: "",
                ),
            )
        },
    )
}

internal fun SyncDevice.toResponse() = SyncDeviceResponse(
    id = id,
    deviceId = deviceId,
    deviceName = deviceName,
    deviceType = deviceType.name,
    lastSyncAt = lastSyncAt?.toString(),
    isActive = isActive,
    createdAt = createdAt.toString(),
)

internal fun SyncChange.toResponse() = SyncChangeResponse(
    id = id,
    itemId = itemId,
    changeType = changeType.name,
    timestamp = timestamp.toString(),
    cursor = cursor,
    oldPath = oldPath,
    newPath = newPath,
    checksum = checksum,
)

internal fun SyncConflict.toResponse() = SyncConflictResponse(
    id = id,
    itemId = itemId,
    conflictType = conflictType.name,
    localChange = localChange.toResponse(),
    remoteChange = remoteChange.toResponse(),
    createdAt = createdAt.toString(),
)
