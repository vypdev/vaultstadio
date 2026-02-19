/**
 * VaultStadio Sync Routes
 *
 * API endpoints for file synchronization operations.
 */

package com.vaultstadio.api.routes.sync

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequest(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
)

@Serializable
data class SyncDeviceResponse(
    val id: String,
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    val lastSyncAt: String?,
    val isActive: Boolean,
    val createdAt: String,
)

@Serializable
data class SyncRequest(
    val cursor: String? = null,
    val limit: Int = 1000,
    val includeDeleted: Boolean = true,
)

@Serializable
data class SyncChangeResponse(
    val id: String,
    val itemId: String,
    val changeType: String,
    val timestamp: String,
    val cursor: Long,
    val oldPath: String?,
    val newPath: String?,
    val checksum: String?,
)

@Serializable
data class SyncResponse(
    val changes: List<SyncChangeResponse>,
    val cursor: String,
    val hasMore: Boolean,
    val conflicts: List<SyncConflictResponse>,
    val serverTime: String,
)

@Serializable
data class SyncConflictResponse(
    val id: String,
    val itemId: String,
    val conflictType: String,
    val localChange: SyncChangeResponse,
    val remoteChange: SyncChangeResponse,
    val createdAt: String,
)

@Serializable
data class PushChangesRequest(
    val changes: List<ClientChange>,
)

@Serializable
data class ClientChange(
    val itemId: String,
    val changeType: String,
    val oldPath: String? = null,
    val newPath: String? = null,
    val checksum: String? = null,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
data class ResolveConflictRequest(
    val resolution: String,
)

@Serializable
data class FileSignatureResponse(
    val itemId: String,
    val blockSize: Int,
    val versionNumber: Int,
    val blocks: List<BlockSignatureResponse>,
)

@Serializable
data class BlockSignatureResponse(
    val index: Int,
    val weakChecksum: Long,
    val strongChecksum: String,
)

@Serializable
data class DeltaUploadRequest(
    val baseVersion: Int,
    val blocks: List<DeltaBlock>,
    val newChecksum: String,
)

@Serializable
data class DeltaBlock(
    val index: Int,
    val operation: String,
    val sourceIndex: Int? = null,
    val data: String? = null,
)

@Serializable
data class DeltaUploadResponse(
    val success: Boolean,
    val itemId: String,
    val appliedBlocks: Int,
    val newVersion: Int,
    val newChecksum: String,
)

/**
 * Configure sync routes.
 */
fun Route.syncRoutes() {
    authenticate("auth-bearer") {
        route("/api/v1/sync") {
            route("/devices") {
                post { handleRegisterDevice(call) }
                get { handleListDevices(call) }
                post("/{deviceId}/deactivate") { handleDeactivateDevice(call) }
                delete("/{deviceId}") { handleRemoveDevice(call) }
            }

            post("/pull") { handlePull(call) }
            post("/push") { handlePush(call) }

            route("/conflicts") {
                get { handleGetPendingConflicts(call) }
                post("/{conflictId}/resolve") { handleResolveConflict(call) }
            }

            route("/delta") {
                get("/signature/{itemId}") { handleGetFileSignature(call) }
                post("/upload/{itemId}") { handleDeltaUpload(call) }
            }
        }
    }
}
