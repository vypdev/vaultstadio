/**
 * VaultStadio Event System
 *
 * Event-driven architecture for plugin communication.
 * The core publishes events, and plugins can subscribe to them.
 */

package com.vaultstadio.core.domain.event

import com.vaultstadio.domain.auth.model.UserInfo
import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.domain.storage.model.StorageItem
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID
import com.vaultstadio.domain.common.event.StorageEvent as DomainStorageEvent

/**
 * Base interface for all storage events (extends domain port).
 */
@Serializable
sealed interface StorageEvent : DomainStorageEvent

/**
 * Events related to file operations.
 */
@Serializable
sealed class FileEvent : StorageEvent {
    abstract val item: StorageItem

    /**
     * Event published when a file is uploaded.
     */
    @Serializable
    data class Uploaded(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String?,
        override val item: StorageItem,
        val contentStream: String? = null, // Reference to temp file for plugin processing
    ) : FileEvent()

    /**
     * Event published when a file is downloaded.
     */
    @Serializable
    data class Downloaded(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String?,
        override val item: StorageItem,
        val accessedViaShare: Boolean = false,
        val shareId: String? = null,
    ) : FileEvent()

    /**
     * Event published when a file is about to be deleted.
     */
    @Serializable
    data class Deleting(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String?,
        override val item: StorageItem,
        val permanent: Boolean = false,
    ) : FileEvent()

    /**
     * Event published after a file is deleted.
     */
    @Serializable
    data class Deleted(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String?,
        override val item: StorageItem,
        val permanent: Boolean = false,
    ) : FileEvent()

    /**
     * Event published when a file is moved.
     */
    @Serializable
    data class Moved(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String?,
        override val item: StorageItem,
        val previousPath: String,
        val previousParentId: String?,
    ) : FileEvent()

    /**
     * Event published when a file is renamed.
     */
    @Serializable
    data class Renamed(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String?,
        override val item: StorageItem,
        val previousName: String,
    ) : FileEvent()

    /**
     * Event published when a file is copied.
     */
    @Serializable
    data class Copied(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String?,
        override val item: StorageItem,
        val sourceItem: StorageItem,
    ) : FileEvent()

    /**
     * Event published when a file is restored from trash.
     */
    @Serializable
    data class Restored(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String?,
        override val item: StorageItem,
    ) : FileEvent()

    /**
     * Event published when a file is starred/unstarred.
     */
    @Serializable
    data class StarredChanged(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String?,
        override val item: StorageItem,
        val isStarred: Boolean,
    ) : FileEvent()
}

/**
 * Events related to folder operations.
 */
@Serializable
sealed class FolderEvent : StorageEvent {
    abstract val folder: StorageItem

    /**
     * Event published when a folder is created.
     */
    @Serializable
    data class Created(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String?,
        override val folder: StorageItem,
    ) : FolderEvent()

    /**
     * Event published when a folder is deleted.
     */
    @Serializable
    data class Deleted(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String?,
        override val folder: StorageItem,
        val itemCount: Int = 0,
    ) : FolderEvent()

    /**
     * Event published when a folder is moved.
     */
    @Serializable
    data class Moved(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String?,
        override val folder: StorageItem,
        val previousPath: String,
    ) : FolderEvent()
}

/**
 * Events related to sharing.
 */
@Serializable
sealed class ShareEvent : StorageEvent {

    /**
     * Event published when a share is created.
     */
    @Serializable
    data class Created(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String?,
        val share: ShareLink,
        val item: StorageItem,
    ) : ShareEvent()

    /**
     * Event published when a share is accessed.
     */
    @Serializable
    data class Accessed(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String?,
        val share: ShareLink,
        val item: StorageItem,
        val ipAddress: String?,
        val userAgent: String?,
    ) : ShareEvent()

    /**
     * Event published when a share is deleted.
     */
    @Serializable
    data class Deleted(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String?,
        val shareId: String,
        val itemId: String,
    ) : ShareEvent()
}

/**
 * Events related to user operations.
 */
@Serializable
sealed class UserEvent : StorageEvent {

    /**
     * Event published when a user logs in.
     */
    @Serializable
    data class LoggedIn(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val ipAddress: String?,
        val userAgent: String?,
    ) : UserEvent()

    /**
     * Event published when a user logs out.
     */
    @Serializable
    data class LoggedOut(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
    ) : UserEvent()

    /**
     * Event published when a user is created.
     */
    @Serializable
    data class Created(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String?,
        val user: UserInfo,
    ) : UserEvent()

    /**
     * Event published when a user's quota changes.
     */
    @Serializable
    data class QuotaChanged(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String,
        val previousQuota: Long?,
        val newQuota: Long?,
    ) : UserEvent()
}

/**
 * System events.
 */
@Serializable
sealed class SystemEvent : StorageEvent {

    /**
     * Event published when a plugin is installed.
     */
    @Serializable
    data class PluginInstalled(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String?,
        val pluginId: String,
        val pluginVersion: String,
    ) : SystemEvent()

    /**
     * Event published when a plugin is uninstalled.
     */
    @Serializable
    data class PluginUninstalled(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String?,
        val pluginId: String,
    ) : SystemEvent()

    /**
     * Event published when maintenance tasks run.
     */
    @Serializable
    data class MaintenanceRun(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val userId: String? = null,
        val taskType: String,
        val details: String?,
    ) : SystemEvent()
}
