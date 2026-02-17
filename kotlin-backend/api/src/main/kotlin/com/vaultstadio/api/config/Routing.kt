/**
 * VaultStadio API Routing Configuration
 *
 * This file configures all API routes for the VaultStadio application.
 * Routes are organized by feature and authentication requirements.
 */

package com.vaultstadio.api.config

import com.vaultstadio.api.routes.activity.activityRoutes
import com.vaultstadio.api.routes.admin.adminRoutes
import com.vaultstadio.api.routes.admin.userRoutes
import com.vaultstadio.api.routes.ai.aiRoutes
import com.vaultstadio.api.routes.auth.authRoutes
import com.vaultstadio.api.routes.collaboration.collaborationRoutes
import com.vaultstadio.api.routes.federation.federationRoutes
import com.vaultstadio.api.routes.health.healthRoutes
import com.vaultstadio.api.routes.metadata.metadataRoutes
import com.vaultstadio.api.routes.metadata.searchRoutes
import com.vaultstadio.api.routes.plugin.pluginRoutes
import com.vaultstadio.api.routes.share.publicShareRoutes
import com.vaultstadio.api.routes.share.shareRoutes
import com.vaultstadio.api.routes.storage.batchRoutes
import com.vaultstadio.api.routes.storage.chunkedUploadRoutes
import com.vaultstadio.api.routes.storage.folderUploadRoutes
import com.vaultstadio.api.routes.storage.s3Routes
import com.vaultstadio.api.routes.storage.storageRoutes
import com.vaultstadio.api.routes.storage.thumbnailRoutes
import com.vaultstadio.api.routes.storage.webDAVRoutes
import com.vaultstadio.api.routes.sync.syncRoutes
import com.vaultstadio.api.routes.version.versionRoutes
import com.vaultstadio.core.domain.service.CollaborationService
import com.vaultstadio.core.domain.service.FederationService
import com.vaultstadio.core.domain.service.FileVersionService
import com.vaultstadio.core.domain.service.LockManager
import com.vaultstadio.core.domain.service.MultipartUploadManagerInterface
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.domain.service.SyncService
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

/**
 * Configures all application routes.
 *
 * Route organization:
 * - Health checks: /health, /ready, /metrics (no auth)
 * - Core API: /api/v1/ (JWT auth)
 * - S3 Compatible: /s3/ (AWS Signature V4 or JWT)
 * - WebDAV: /webdav/ (Basic Auth or JWT)
 * - Phase 6 features: versioning, sync, federation, collaboration
 */
fun Application.configureRouting() {
    // Inject Phase 6 services and config
    val appConfig: AppConfig by inject()
    val storageService: StorageService by inject()
    val fileVersionService: FileVersionService by inject()
    val syncService: SyncService by inject()
    val federationService: FederationService by inject()
    val collaborationService: CollaborationService by inject()
    val lockManager: LockManager by inject()
    val multipartUploadManager: MultipartUploadManagerInterface by inject()

    routing {
        // Health Check Routes (no authentication required)
        healthRoutes()

        // S3-Compatible API (Phase 6) – only when storage is S3 or MinIO
        // Authentication: AWS Signature V4 or JWT fallback
        if (appConfig.storage.type == StorageType.S3 || appConfig.storage.type == StorageType.MINIO) {
            s3Routes(storageService, multipartUploadManager)
        }

        // WebDAV Protocol (Phase 6)
        // Authentication: HTTP Basic Auth or JWT fallback
        webDAVRoutes(storageService, lockManager)

        // Federation Protocol (Phase 6)
        // Some routes are public (well-known, incoming requests)
        federationRoutes(federationService)

        // Core API v1 Routes
        route("/api/v1") {
            // Public routes (no authentication required)
            authRoutes()
            publicShareRoutes()

            // Protected routes (JWT authentication required)
            authenticate("auth-bearer") {
                // Core storage operations
                storageRoutes()
                batchRoutes()
                chunkedUploadRoutes()
                folderUploadRoutes()
                thumbnailRoutes()
                metadataRoutes()

                // User and sharing
                userRoutes()
                shareRoutes()
                searchRoutes()
                activityRoutes()

                // Administration
                pluginRoutes()
                aiRoutes()
                adminRoutes()
            }
        }

        // Phase 6: Advanced Features Routes
        // These routes define their own paths and authentication

        // File Versioning: /api/v1/versions
        versionRoutes(fileVersionService)

        // Sync Protocol: /api/v1/sync
        syncRoutes(syncService)

        // Real-time Collaboration: /api/v1/collaboration
        collaborationRoutes(collaborationService)
    }
}
