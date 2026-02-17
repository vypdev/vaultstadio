/**
 * VaultStadio Database Tables
 */

package com.vaultstadio.infrastructure.persistence.entities

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Users table.
 */
object UsersTable : Table("users") {
    val id = varchar("id", 36)
    val email = varchar("email", 255).uniqueIndex()
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 20)
    val status = varchar("status", 20)
    val quotaBytes = long("quota_bytes").nullable()
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val preferences = text("preferences").nullable()
    val lastLoginAt = timestamp("last_login_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

/**
 * User sessions table.
 */
object UserSessionsTable : Table("user_sessions") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
    val tokenHash = varchar("token_hash", 64).index()
    val refreshTokenHash = varchar("refresh_token_hash", 64).nullable().index()
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = varchar("user_agent", 500).nullable()
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
    val lastActivityAt = timestamp("last_activity_at")

    override val primaryKey = PrimaryKey(id)
}

/**
 * API keys table.
 */
object ApiKeysTable : Table("api_keys") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
    val name = varchar("name", 100)
    val keyHash = varchar("key_hash", 64).index()
    val permissions = text("permissions")
    val expiresAt = timestamp("expires_at").nullable()
    val lastUsedAt = timestamp("last_used_at").nullable()
    val createdAt = timestamp("created_at")
    val isActive = bool("is_active")

    override val primaryKey = PrimaryKey(id)
}

/**
 * Storage items table.
 */
object StorageItemsTable : Table("storage_items") {
    val id = varchar("id", 36)
    val name = varchar("name", 255)
    val path = varchar("path", 4096)
    val type = varchar("type", 10)
    val parentId = varchar("parent_id", 36).nullable()
    val ownerId = varchar("owner_id", 36)
    val size = long("size")
    val mimeType = varchar("mime_type", 255).nullable()
    val checksum = varchar("checksum", 64).nullable()
    val storageKey = varchar("storage_key", 500).nullable()
    val visibility = varchar("visibility", 20)
    val isTrashed = bool("is_trashed")
    val isStarred = bool("is_starred")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val trashedAt = timestamp("trashed_at").nullable()
    val version = long("version")

    override val primaryKey = PrimaryKey(id)

    init {
        index(true, ownerId, path) // Unique path per owner
        index(false, parentId)
        index(false, ownerId, isTrashed)
        index(false, ownerId, isStarred)
    }
}

/**
 * Storage item metadata table.
 */
object StorageItemMetadataTable : Table("storage_item_metadata") {
    val id = varchar("id", 36)
    val itemId = varchar("item_id", 36)
    val pluginId = varchar("plugin_id", 100)
    val key = varchar("key", 100)
    val value = text("value")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(true, itemId, pluginId, key) // Unique key per item per plugin
        index(false, pluginId)
    }
}

/**
 * Share links table.
 */
object ShareLinksTable : Table("share_links") {
    val id = varchar("id", 36)
    val itemId = varchar("item_id", 36)
    val token = varchar("token", 64).uniqueIndex()
    val createdBy = varchar("created_by", 36)
    val expiresAt = timestamp("expires_at").nullable()
    val password = varchar("password", 255).nullable()
    val maxDownloads = integer("max_downloads").nullable()
    val downloadCount = integer("download_count")
    val isActive = bool("is_active")
    val createdAt = timestamp("created_at")
    val sharedWithUsers = text("shared_with_users").default("")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, itemId)
        index(false, createdBy)
    }
}

/**
 * Activities table.
 */
object ActivitiesTable : Table("activities") {
    val id = varchar("id", 36)
    val type = varchar("type", 50)
    val userId = varchar("user_id", 36).nullable()
    val itemId = varchar("item_id", 36).nullable()
    val itemPath = varchar("item_path", 4096).nullable()
    val details = text("details").nullable()
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = varchar("user_agent", 500).nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, userId)
        index(false, itemId)
        index(false, createdAt)
    }
}

// ============================================================================
// Phase 6: File Versioning Tables
// ============================================================================

/**
 * File versions table.
 */
object FileVersionsTable : Table("file_versions") {
    val id = varchar("id", 36)
    val itemId = varchar("item_id", 36)
    val versionNumber = integer("version_number")
    val size = long("size")
    val checksum = varchar("checksum", 64)
    val storageKey = varchar("storage_key", 500)
    val createdBy = varchar("created_by", 36)
    val createdAt = timestamp("created_at")
    val comment = text("comment").nullable()
    val isLatest = bool("is_latest")
    val restoredFrom = integer("restored_from").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(true, itemId, versionNumber) // Unique version per item
        index(false, itemId, isLatest)
        index(false, storageKey)
    }
}

// ============================================================================
// Phase 6: Sync Tables
// ============================================================================

/**
 * Sync devices table.
 */
object SyncDevicesTable : Table("sync_devices") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
    val deviceId = varchar("device_id", 100)
    val deviceName = varchar("device_name", 255)
    val deviceType = varchar("device_type", 50)
    val lastSyncAt = timestamp("last_sync_at").nullable()
    val lastSyncCursor = varchar("last_sync_cursor", 50).nullable()
    val isActive = bool("is_active")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(true, userId, deviceId) // Unique device per user
        index(false, userId, isActive)
    }
}

/**
 * Sync changes table.
 */
object SyncChangesTable : Table("sync_changes") {
    val id = varchar("id", 36)
    val itemId = varchar("item_id", 36)
    val changeType = varchar("change_type", 20)
    val userId = varchar("user_id", 36)
    val deviceId = varchar("device_id", 36).nullable()
    val timestamp = timestamp("timestamp")
    val cursor = long("cursor")
    val oldPath = varchar("old_path", 4096).nullable()
    val newPath = varchar("new_path", 4096).nullable()
    val checksum = varchar("checksum", 64).nullable()
    val metadata = text("metadata").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, userId, cursor)
        index(false, itemId)
        index(false, timestamp)
    }
}

/**
 * Sync conflicts table.
 */
object SyncConflictsTable : Table("sync_conflicts") {
    val id = varchar("id", 36)
    val itemId = varchar("item_id", 36)
    val localChangeId = varchar("local_change_id", 36)
    val remoteChangeId = varchar("remote_change_id", 36)
    val conflictType = varchar("conflict_type", 30)
    val resolvedAt = timestamp("resolved_at").nullable()
    val resolution = varchar("resolution", 20).nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, itemId)
        index(false, resolvedAt)
    }
}

// ============================================================================
// Phase 6: Collaboration Tables
// ============================================================================

/**
 * Collaboration sessions table.
 */
object CollaborationSessionsTable : Table("collaboration_sessions") {
    val id = varchar("id", 36)
    val itemId = varchar("item_id", 36)
    val createdAt = timestamp("created_at")
    val expiresAt = timestamp("expires_at")
    val closedAt = timestamp("closed_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, itemId)
        index(false, expiresAt)
    }
}

/**
 * Collaboration participants table.
 */
object CollaborationParticipantsTable : Table("collaboration_participants") {
    val id = varchar("id", 36)
    val sessionId = varchar("session_id", 36)
    val userId = varchar("user_id", 36)
    val userName = varchar("user_name", 100)
    val color = varchar("color", 7)
    val cursorLine = integer("cursor_line").nullable()
    val cursorColumn = integer("cursor_column").nullable()
    val cursorOffset = integer("cursor_offset").nullable()
    val selectionStartLine = integer("selection_start_line").nullable()
    val selectionStartColumn = integer("selection_start_column").nullable()
    val selectionStartOffset = integer("selection_start_offset").nullable()
    val selectionEndLine = integer("selection_end_line").nullable()
    val selectionEndColumn = integer("selection_end_column").nullable()
    val selectionEndOffset = integer("selection_end_offset").nullable()
    val joinedAt = timestamp("joined_at")
    val lastActiveAt = timestamp("last_active_at")
    val isEditing = bool("is_editing")
    val leftAt = timestamp("left_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, sessionId)
        index(false, userId)
    }
}

/**
 * Document states table (for OT).
 */
object DocumentStatesTable : Table("document_states") {
    val id = varchar("id", 36)
    val itemId = varchar("item_id", 36).uniqueIndex()
    val version = long("version")
    val content = text("content")
    val lastModified = timestamp("last_modified")

    override val primaryKey = PrimaryKey(id)
}

/**
 * Collaboration operations table (OT history).
 */
object CollaborationOperationsTable : Table("collaboration_operations") {
    val id = varchar("id", 36)
    val itemId = varchar("item_id", 36)
    val userId = varchar("user_id", 36)
    val operationType = varchar("operation_type", 20)
    val position = integer("position")
    val text = text("text").nullable()
    val length = integer("length").nullable()
    val baseVersion = long("base_version")
    val timestamp = timestamp("timestamp")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, itemId, baseVersion)
    }
}

/**
 * User presence table.
 */
object UserPresenceTable : Table("user_presence") {
    val userId = varchar("user_id", 36)
    val status = varchar("status", 20)
    val lastSeen = timestamp("last_seen")
    val activeSession = varchar("active_session", 36).nullable()
    val activeDocument = varchar("active_document", 36).nullable()

    override val primaryKey = PrimaryKey(userId)
}

/**
 * Document comments table.
 */
object DocumentCommentsTable : Table("document_comments") {
    val id = varchar("id", 36)
    val itemId = varchar("item_id", 36)
    val userId = varchar("user_id", 36)
    val content = text("content")
    val anchorStartLine = integer("anchor_start_line")
    val anchorStartColumn = integer("anchor_start_column")
    val anchorEndLine = integer("anchor_end_line")
    val anchorEndColumn = integer("anchor_end_column")
    val quotedText = text("quoted_text").nullable()
    val resolvedAt = timestamp("resolved_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, itemId)
        index(false, userId)
    }
}

/**
 * Comment replies table.
 */
object CommentRepliesTable : Table("comment_replies") {
    val id = varchar("id", 36)
    val commentId = varchar("comment_id", 36)
    val userId = varchar("user_id", 36)
    val content = text("content")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, commentId)
    }
}

// ============================================================================
// Phase 6: Federation Tables
// ============================================================================

/**
 * Federated instances table.
 */
object FederatedInstancesTable : Table("federated_instances") {
    val id = varchar("id", 36)
    val domain = varchar("domain", 255).uniqueIndex()
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val version = varchar("version", 50)
    val publicKey = text("public_key")
    val capabilities = text("capabilities") // JSON array
    val status = varchar("status", 20)
    val lastSeenAt = timestamp("last_seen_at").nullable()
    val registeredAt = timestamp("registered_at")
    val metadata = text("metadata").nullable() // JSON object

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, status)
    }
}

/**
 * Federated shares table.
 */
object FederatedSharesTable : Table("federated_shares") {
    val id = varchar("id", 36)
    val itemId = varchar("item_id", 36)
    val sourceInstance = varchar("source_instance", 255)
    val targetInstance = varchar("target_instance", 255)
    val targetUserId = varchar("target_user_id", 100).nullable()
    val permissions = text("permissions") // JSON array
    val expiresAt = timestamp("expires_at").nullable()
    val createdBy = varchar("created_by", 36)
    val createdAt = timestamp("created_at")
    val acceptedAt = timestamp("accepted_at").nullable()
    val status = varchar("status", 20)

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, itemId)
        index(false, createdBy)
        index(false, targetInstance)
        index(false, status)
    }
}

/**
 * Federated identities table.
 */
object FederatedIdentitiesTable : Table("federated_identities") {
    val id = varchar("id", 36)
    val localUserId = varchar("local_user_id", 36).nullable()
    val remoteUserId = varchar("remote_user_id", 100)
    val remoteInstance = varchar("remote_instance", 255)
    val displayName = varchar("display_name", 255)
    val email = varchar("email", 255).nullable()
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val verified = bool("verified")
    val linkedAt = timestamp("linked_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(true, remoteUserId, remoteInstance) // Unique per remote
        index(false, localUserId)
    }
}

/**
 * Federated activities table.
 */
object FederatedActivitiesTable : Table("federated_activities") {
    val id = varchar("id", 36)
    val instanceDomain = varchar("instance_domain", 255)
    val activityType = varchar("activity_type", 30)
    val actorId = varchar("actor_id", 255)
    val objectId = varchar("object_id", 255)
    val objectType = varchar("object_type", 50)
    val summary = text("summary")
    val timestamp = timestamp("timestamp")
    val metadata = text("metadata").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, instanceDomain)
        index(false, timestamp)
    }
}
