/**
 * VaultStadio File Version Routes
 *
 * API endpoints for file versioning operations.
 */

package com.vaultstadio.api.routes.version

import com.vaultstadio.application.usecase.version.ApplyRetentionPolicyUseCase
import com.vaultstadio.application.usecase.version.CompareVersionsUseCase
import com.vaultstadio.application.usecase.version.DeleteVersionUseCase
import com.vaultstadio.application.usecase.version.GetVersionHistoryUseCase
import com.vaultstadio.application.usecase.version.GetVersionUseCase
import com.vaultstadio.application.usecase.version.RestoreVersionUseCase
import com.vaultstadio.api.config.user
import com.vaultstadio.core.domain.model.VersionRetentionPolicy
import com.vaultstadio.core.domain.service.RestoreVersionInput
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.get as koinGet

/**
 * Request to create a new version.
 */
@Serializable
data class CreateVersionRequest(
    val comment: String? = null,
)

/**
 * Request to restore a version.
 */
@Serializable
data class RestoreVersionRequest(
    val versionNumber: Int,
    val comment: String? = null,
)

/**
 * File version response.
 */
@Serializable
data class FileVersionResponse(
    val id: String,
    val itemId: String,
    val versionNumber: Int,
    val size: Long,
    val checksum: String,
    val createdBy: String,
    val createdAt: String,
    val comment: String?,
    val isLatest: Boolean,
    val restoredFrom: Int?,
)

/**
 * Version history response.
 */
@Serializable
data class VersionHistoryResponse(
    val itemId: String,
    val itemName: String,
    val versions: List<FileVersionResponse>,
    val totalVersions: Int,
    val totalSize: Long,
)

/**
 * Version diff response.
 */
@Serializable
data class VersionDiffResponse(
    val fromVersion: Int,
    val toVersion: Int,
    val sizeChange: Long,
    val additions: Int,
    val deletions: Int,
    val isBinary: Boolean,
    val patches: List<DiffPatchResponse>,
)

/**
 * Diff patch response.
 */
@Serializable
data class DiffPatchResponse(
    val operation: String,
    val startLine: Int,
    val endLine: Int,
    val oldContent: String?,
    val newContent: String?,
)

/**
 * Request for retention policy cleanup.
 */
@Serializable
data class CleanupRequest(
    val maxVersions: Int? = null,
    val maxAgeDays: Int? = null,
    val minVersionsToKeep: Int = 1,
)

/**
 * Configure version routes.
 */
fun Route.versionRoutes() {
    authenticate("auth-bearer") {
        route("/api/v1/versions") {
            get("/item/{itemId}") {
                val getVersionHistoryUseCase: GetVersionHistoryUseCase = call.application.koinGet()
                val itemId = call.parameters["itemId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing item ID")

                getVersionHistoryUseCase(itemId).fold(
                    { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                    { history ->
                        call.respond(
                            VersionHistoryResponse(
                                itemId = history.item.id,
                                itemName = history.item.name,
                                versions = history.versions.map { it.toResponse() },
                                totalVersions = history.totalVersions,
                                totalSize = history.totalSize,
                            ),
                        )
                    },
                )
            }

            get("/item/{itemId}/version/{versionNumber}") {
                val getVersionUseCase: GetVersionUseCase = call.application.koinGet()
                val itemId = call.parameters["itemId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing item ID")
                val versionNumber = call.parameters["versionNumber"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid version number")

                getVersionUseCase(itemId, versionNumber).fold(
                    { error -> call.respond(HttpStatusCode.NotFound, error.message ?: "Version not found") },
                    { version ->
                        if (version != null) {
                            call.respond(version.toResponse())
                        } else {
                            call.respond(HttpStatusCode.NotFound, "Version not found")
                        }
                    },
                )
            }

            get("/item/{itemId}/version/{versionNumber}/download") {
                val getVersionUseCase: GetVersionUseCase = call.application.koinGet()
                val itemId = call.parameters["itemId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing item ID")
                val versionNumber = call.parameters["versionNumber"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid version number")

                getVersionUseCase(itemId, versionNumber).fold(
                    { error -> call.respond(HttpStatusCode.NotFound, error.message ?: "Version not found") },
                    { version ->
                        if (version != null) {
                            // Return download info - actual download would be handled by storage service
                            call.respond(
                                mapOf(
                                    "versionId" to version.id,
                                    "storageKey" to version.storageKey,
                                    "size" to version.size,
                                    "checksum" to version.checksum,
                                ),
                            )
                        } else {
                            call.respond(HttpStatusCode.NotFound, "Version not found")
                        }
                    },
                )
            }

            post("/item/{itemId}/restore") {
                val restoreVersionUseCase: RestoreVersionUseCase = call.application.koinGet()
                val itemId = call.parameters["itemId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing item ID")
                val request = call.receive<RestoreVersionRequest>()
                val user = call.user ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val userId = user.id

                val input = RestoreVersionInput(
                    itemId = itemId,
                    versionNumber = request.versionNumber,
                    comment = request.comment,
                )

                restoreVersionUseCase(input, userId).fold(
                    { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                    { version ->
                        call.respond(
                            HttpStatusCode.OK,
                            mapOf(
                                "message" to "Version restored",
                                "newVersion" to version.toResponse(),
                            ),
                        )
                    },
                )
            }

            get("/item/{itemId}/diff") {
                val compareVersionsUseCase: CompareVersionsUseCase = call.application.koinGet()
                val itemId = call.parameters["itemId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing item ID")
                val fromVersion = call.request.queryParameters["from"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing 'from' version")
                val toVersion = call.request.queryParameters["to"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing 'to' version")

                compareVersionsUseCase(itemId, fromVersion, toVersion).fold(
                    { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                    { diff ->
                        call.respond(
                            VersionDiffResponse(
                                fromVersion = diff.fromVersion,
                                toVersion = diff.toVersion,
                                sizeChange = diff.sizeChange,
                                additions = diff.additions,
                                deletions = diff.deletions,
                                isBinary = diff.isBinary,
                                patches = diff.patches.map { patch ->
                                    DiffPatchResponse(
                                        operation = patch.operation.name,
                                        startLine = patch.startLine,
                                        endLine = patch.endLine,
                                        oldContent = patch.oldContent,
                                        newContent = patch.newContent,
                                    )
                                },
                            ),
                        )
                    },
                )
            }

            delete("/{versionId}") {
                val deleteVersionUseCase: DeleteVersionUseCase = call.application.koinGet()
                val versionId = call.parameters["versionId"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing version ID")
                val user = call.user ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                val userId = user.id

                deleteVersionUseCase(versionId, userId).fold(
                    { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                    { call.respond(HttpStatusCode.NoContent) },
                )
            }

            post("/item/{itemId}/cleanup") {
                val applyRetentionPolicyUseCase: ApplyRetentionPolicyUseCase = call.application.koinGet()
                val itemId = call.parameters["itemId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing item ID")
                val request = call.receive<CleanupRequest>()

                val policy = VersionRetentionPolicy(
                    maxVersions = request.maxVersions,
                    maxAgeDays = request.maxAgeDays,
                    minVersionsToKeep = request.minVersionsToKeep,
                )

                applyRetentionPolicyUseCase(itemId, policy).fold(
                    { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
                    { deletedIds ->
                        call.respond(
                            HttpStatusCode.OK,
                            mapOf(
                                "message" to "Retention policy applied",
                                "deletedVersions" to deletedIds,
                            ),
                        )
                    },
                )
            }
        }
    }
}

private fun com.vaultstadio.core.domain.model.FileVersion.toResponse() = FileVersionResponse(
    id = id,
    itemId = itemId,
    versionNumber = versionNumber,
    size = size,
    checksum = checksum,
    createdBy = createdBy,
    createdAt = createdAt.toString(),
    comment = comment,
    isLatest = isLatest,
    restoredFrom = restoredFrom,
)
