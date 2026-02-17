/**
 * VaultStadio Activity Logger
 *
 * Automatically logs user activities by subscribing to system events.
 * Converts events into Activity records for audit logging.
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.event.EventHandlerResult
import com.vaultstadio.core.domain.event.EventSubscription
import com.vaultstadio.core.domain.event.FileEvent
import com.vaultstadio.core.domain.event.FolderEvent
import com.vaultstadio.core.domain.event.ShareEvent
import com.vaultstadio.core.domain.event.SystemEvent
import com.vaultstadio.core.domain.event.UserEvent
import com.vaultstadio.core.domain.model.Activity
import com.vaultstadio.core.domain.model.ActivityType
import com.vaultstadio.core.domain.repository.ActivityRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val logger = KotlinLogging.logger {}

/**
 * Service that automatically logs user activities by listening to events.
 *
 * @property eventBus The event bus to subscribe to
 * @property activityRepository Repository for persisting activities
 */
class ActivityLogger(
    private val eventBus: EventBus,
    private val activityRepository: ActivityRepository,
) {
    private val handlerId = "activity-logger"
    private val subscriptions = mutableListOf<EventSubscription>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val json = Json {
        prettyPrint = false
        encodeDefaults = false
    }

    /**
     * Starts the activity logger by subscribing to all relevant events.
     */
    fun start() {
        logger.info { "Starting Activity Logger..." }

        // Subscribe to file events
        subscriptions.add(
            eventBus.subscribe<FileEvent.Uploaded>(handlerId) { event ->
                logActivity(
                    type = ActivityType.FILE_UPLOADED,
                    userId = event.userId,
                    itemId = event.item.id,
                    itemPath = event.item.path,
                    details = json.encodeToString(
                        buildJsonObject {
                            put("fileName", JsonPrimitive(event.item.name))
                            put("mimeType", JsonPrimitive(event.item.mimeType ?: ""))
                            put("size", JsonPrimitive(event.item.size))
                        },
                    ),
                )
            },
        )

        subscriptions.add(
            eventBus.subscribe<FileEvent.Downloaded>(handlerId) { event ->
                logActivity(
                    type = ActivityType.FILE_DOWNLOADED,
                    userId = event.userId,
                    itemId = event.item.id,
                    itemPath = event.item.path,
                    details = if (event.accessedViaShare) {
                        json.encodeToString(buildJsonObject { put("shareId", JsonPrimitive(event.shareId)) })
                    } else {
                        null
                    },
                )
            },
        )

        subscriptions.add(
            eventBus.subscribe<FileEvent.Deleted>(handlerId) { event ->
                logActivity(
                    type = ActivityType.FILE_DELETED,
                    userId = event.userId,
                    itemId = event.item.id,
                    itemPath = event.item.path,
                    details = json.encodeToString(buildJsonObject { put("permanent", JsonPrimitive(event.permanent)) }),
                )
            },
        )

        subscriptions.add(
            eventBus.subscribe<FileEvent.Moved>(handlerId) { event ->
                logActivity(
                    type = ActivityType.FILE_MOVED,
                    userId = event.userId,
                    itemId = event.item.id,
                    itemPath = event.item.path,
                    details = json.encodeToString(
                        buildJsonObject {
                            put("previousPath", JsonPrimitive(event.previousPath))
                            put("newPath", JsonPrimitive(event.item.path))
                        },
                    ),
                )
            },
        )

        subscriptions.add(
            eventBus.subscribe<FileEvent.Renamed>(handlerId) { event ->
                logActivity(
                    type = ActivityType.FILE_RENAMED,
                    userId = event.userId,
                    itemId = event.item.id,
                    itemPath = event.item.path,
                    details = json.encodeToString(
                        buildJsonObject {
                            put("previousName", JsonPrimitive(event.previousName))
                            put("newName", JsonPrimitive(event.item.name))
                        },
                    ),
                )
            },
        )

        subscriptions.add(
            eventBus.subscribe<FileEvent.Copied>(handlerId) { event ->
                logActivity(
                    type = ActivityType.FILE_COPIED,
                    userId = event.userId,
                    itemId = event.item.id,
                    itemPath = event.item.path,
                    details = json.encodeToString(
                        buildJsonObject {
                            put("sourceId", JsonPrimitive(event.sourceItem.id))
                            put("sourcePath", JsonPrimitive(event.sourceItem.path))
                        },
                    ),
                )
            },
        )

        subscriptions.add(
            eventBus.subscribe<FileEvent.Restored>(handlerId) { event ->
                logActivity(
                    type = ActivityType.FILE_RESTORED,
                    userId = event.userId,
                    itemId = event.item.id,
                    itemPath = event.item.path,
                )
            },
        )

        // Subscribe to folder events
        subscriptions.add(
            eventBus.subscribe<FolderEvent.Created>(handlerId) { event ->
                logActivity(
                    type = ActivityType.FOLDER_CREATED,
                    userId = event.userId,
                    itemId = event.folder.id,
                    itemPath = event.folder.path,
                )
            },
        )

        subscriptions.add(
            eventBus.subscribe<FolderEvent.Deleted>(handlerId) { event ->
                logActivity(
                    type = ActivityType.FOLDER_DELETED,
                    userId = event.userId,
                    itemId = event.folder.id,
                    itemPath = event.folder.path,
                    details = json.encodeToString(buildJsonObject { put("itemCount", JsonPrimitive(event.itemCount)) }),
                )
            },
        )

        subscriptions.add(
            eventBus.subscribe<FolderEvent.Moved>(handlerId) { event ->
                logActivity(
                    type = ActivityType.FOLDER_MOVED,
                    userId = event.userId,
                    itemId = event.folder.id,
                    itemPath = event.folder.path,
                    details = json.encodeToString(
                        buildJsonObject {
                            put("previousPath", JsonPrimitive(event.previousPath))
                        },
                    ),
                )
            },
        )

        // Subscribe to share events
        subscriptions.add(
            eventBus.subscribe<ShareEvent.Created>(handlerId) { event ->
                logActivity(
                    type = ActivityType.SHARE_CREATED,
                    userId = event.userId,
                    itemId = event.item.id,
                    itemPath = event.item.path,
                    details = json.encodeToString(
                        buildJsonObject {
                            put("shareId", JsonPrimitive(event.share.id))
                            put("hasPassword", JsonPrimitive(event.share.password != null))
                            put("hasExpiration", JsonPrimitive(event.share.expiresAt != null))
                        },
                    ),
                )
            },
        )

        subscriptions.add(
            eventBus.subscribe<ShareEvent.Accessed>(handlerId) { event ->
                logActivity(
                    type = ActivityType.SHARE_ACCESSED,
                    userId = event.userId,
                    itemId = event.item.id,
                    itemPath = event.item.path,
                    details = json.encodeToString(buildJsonObject { put("shareId", JsonPrimitive(event.share.id)) }),
                    ipAddress = event.ipAddress,
                    userAgent = event.userAgent,
                )
            },
        )

        subscriptions.add(
            eventBus.subscribe<ShareEvent.Deleted>(handlerId) { event ->
                logActivity(
                    type = ActivityType.SHARE_DELETED,
                    userId = event.userId,
                    itemId = event.itemId,
                    details = json.encodeToString(buildJsonObject { put("shareId", JsonPrimitive(event.shareId)) }),
                )
            },
        )

        // Subscribe to user events
        subscriptions.add(
            eventBus.subscribe<UserEvent.LoggedIn>(handlerId) { event ->
                logActivity(
                    type = ActivityType.USER_LOGIN,
                    userId = event.userId,
                    ipAddress = event.ipAddress,
                    userAgent = event.userAgent,
                )
            },
        )

        subscriptions.add(
            eventBus.subscribe<UserEvent.LoggedOut>(handlerId) { event ->
                logActivity(
                    type = ActivityType.USER_LOGOUT,
                    userId = event.userId,
                )
            },
        )

        subscriptions.add(
            eventBus.subscribe<UserEvent.Created>(handlerId) { event ->
                logActivity(
                    type = ActivityType.USER_CREATED,
                    userId = event.userId,
                    details = json.encodeToString(
                        buildJsonObject {
                            put("newUserId", JsonPrimitive(event.user.id))
                            put("email", JsonPrimitive(event.user.email))
                        },
                    ),
                )
            },
        )

        // Subscribe to system events
        subscriptions.add(
            eventBus.subscribe<SystemEvent.PluginInstalled>(handlerId) { event ->
                logActivity(
                    type = ActivityType.PLUGIN_INSTALLED,
                    userId = event.userId,
                    details = json.encodeToString(
                        buildJsonObject {
                            put("pluginId", JsonPrimitive(event.pluginId))
                            put("version", JsonPrimitive(event.pluginVersion))
                        },
                    ),
                )
            },
        )

        subscriptions.add(
            eventBus.subscribe<SystemEvent.PluginUninstalled>(handlerId) { event ->
                logActivity(
                    type = ActivityType.PLUGIN_UNINSTALLED,
                    userId = event.userId,
                    details = json.encodeToString(buildJsonObject { put("pluginId", JsonPrimitive(event.pluginId)) }),
                )
            },
        )

        logger.info { "Activity Logger started with ${subscriptions.size} subscriptions" }
    }

    /**
     * Stops the activity logger and unsubscribes from all events.
     */
    fun stop() {
        logger.info { "Stopping Activity Logger..." }
        eventBus.unsubscribeAll(handlerId)
        subscriptions.clear()
        scope.cancel()
        logger.info { "Activity Logger stopped" }
    }

    /**
     * Creates an activity log entry.
     */
    private suspend fun logActivity(
        type: ActivityType,
        userId: String?,
        itemId: String? = null,
        itemPath: String? = null,
        details: String? = null,
        ipAddress: String? = null,
        userAgent: String? = null,
    ): EventHandlerResult {
        val activity = Activity(
            type = type,
            userId = userId,
            itemId = itemId,
            itemPath = itemPath,
            details = details,
            ipAddress = ipAddress,
            userAgent = userAgent,
            createdAt = kotlinx.datetime.Clock.System.now(),
        )

        return when (val result = activityRepository.create(activity)) {
            is Either.Left -> {
                logger.error { "Failed to log activity: ${result.value.message}" }
                EventHandlerResult.Error(result.value)
            }
            is Either.Right -> {
                logger.debug { "Logged activity: ${type.name} for user $userId" }
                EventHandlerResult.Success
            }
        }
    }
}
