package com.vaultstadio.app.feature.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.vaultstadio.app.feature.activity.ActivityContent
import com.vaultstadio.app.feature.admin.AdminContent
import com.vaultstadio.app.feature.ai.AIContent
import com.vaultstadio.app.feature.changepassword.ChangePasswordContent
import com.vaultstadio.app.feature.collaboration.CollaborationContent
import com.vaultstadio.app.feature.federation.FederationContent
import com.vaultstadio.app.feature.files.FilesContent
import com.vaultstadio.app.feature.files.FilesMode
import com.vaultstadio.app.feature.licenses.LicensesContent
import com.vaultstadio.app.feature.plugins.PluginsContent
import com.vaultstadio.app.feature.profile.ProfileContent
import com.vaultstadio.app.feature.security.SecurityContent
import com.vaultstadio.app.feature.settings.SettingsContent
import com.vaultstadio.app.feature.sharedwithme.SharedWithMeContent
import com.vaultstadio.app.feature.shares.SharesContent
import com.vaultstadio.app.feature.sync.SyncContent
import com.vaultstadio.app.feature.upload.LocalUploadManager
import com.vaultstadio.app.feature.upload.UploadManager
import com.vaultstadio.app.feature.versionhistory.VersionHistoryContent
import com.vaultstadio.app.navigation.MainDestination
import com.vaultstadio.app.navigation.RoutePaths
import com.vaultstadio.app.platform.setOnPopState
import com.vaultstadio.app.platform.setPath
import com.vaultstadio.app.ui.components.files.UploadBanner
import com.vaultstadio.app.ui.components.layout.MainSidebar
import org.koin.compose.koinInject

/**
 * Main content layout with sidebar navigation.
 */
@Composable
fun MainContent(
    component: MainComponent,
    modifier: Modifier = Modifier,
) {
    val childStack by component.stack.subscribeAsState()
    val currentChild = childStack.active.instance

    // Determine current destination for sidebar highlighting
    val currentDestination = when (currentChild) {
        is MainComponent.Child.Files -> when (currentChild.mode) {
            FilesMode.ALL -> MainDestination.FILES
            FilesMode.RECENT -> MainDestination.RECENT
            FilesMode.STARRED -> MainDestination.STARRED
            FilesMode.TRASH -> MainDestination.TRASH
        }
        is MainComponent.Child.Shares -> MainDestination.SHARED
        is MainComponent.Child.SharedWithMe -> MainDestination.SHARED_WITH_ME
        is MainComponent.Child.Settings -> MainDestination.SETTINGS
        is MainComponent.Child.Profile -> MainDestination.PROFILE
        is MainComponent.Child.Admin -> MainDestination.ADMIN
        is MainComponent.Child.Activity -> MainDestination.ACTIVITY
        is MainComponent.Child.Plugins -> MainDestination.PLUGINS
        is MainComponent.Child.AI -> MainDestination.AI
        is MainComponent.Child.Sync -> MainDestination.SYNC
        is MainComponent.Child.Federation -> MainDestination.FEDERATION
        is MainComponent.Child.Collaboration -> MainDestination.COLLABORATION
        is MainComponent.Child.VersionHistory -> MainDestination.VERSION_HISTORY
        is MainComponent.Child.ChangePassword -> MainDestination.CHANGE_PASSWORD
        is MainComponent.Child.Security -> MainDestination.SECURITY
        is MainComponent.Child.Licenses -> MainDestination.LICENSES
    }

    // Sync URL with current destination (web: updates browser bar; other platforms: no-op)
    LaunchedEffect(currentDestination) {
        setPath(RoutePaths.toPath(currentDestination))
    }

    // Handle browser back/forward (web only)
    LaunchedEffect(component) {
        setOnPopState {
            val path = com.vaultstadio.app.platform.getInitialPath()
            component.navigateTo(RoutePaths.fromPath(path) ?: MainDestination.FILES)
        }
    }

    val uploadManager: UploadManager = koinInject()
    val uploadItems by uploadManager.items.collectAsState(initial = emptyList())
    val uploadMinimized by uploadManager.isMinimized.collectAsState(initial = false)

    // Clear upload destination when user navigates away from Files screen
    LaunchedEffect(currentChild) {
        if (currentChild !is MainComponent.Child.Files) {
            uploadManager.setUploadDestination(null)
        }
    }

    CompositionLocalProvider(LocalUploadManager provides uploadManager) {
        Box(modifier = modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Sidebar
                MainSidebar(
                    currentDestination = currentDestination,
                    isAdmin = component.isAdmin,
                    currentUser = component.currentUser,
                    onNavigate = component::navigateTo,
                    onLogout = component::logout,
                )

                // Content area
                Children(
                    stack = component.stack,
                    modifier = Modifier.weight(1f),
                    animation = stackAnimation(fade()),
                ) { child ->
                    when (val instance = child.instance) {
                        is MainComponent.Child.Files -> FilesContent(component = instance.component)
                        is MainComponent.Child.Shares -> SharesContent(component = instance.component)
                        is MainComponent.Child.SharedWithMe -> SharedWithMeContent(component = instance.component)
                        is MainComponent.Child.Settings -> SettingsContent(component = instance.component)
                        is MainComponent.Child.Profile -> ProfileContent(component = instance.component)
                        is MainComponent.Child.Admin -> AdminContent(component = instance.component)
                        is MainComponent.Child.Activity -> ActivityContent(component = instance.component)
                        is MainComponent.Child.Plugins -> PluginsContent(component = instance.component)
                        is MainComponent.Child.AI -> AIContent(component = instance.component)
                        is MainComponent.Child.Sync -> SyncContent(component = instance.component)
                        is MainComponent.Child.Federation -> FederationContent(component = instance.component)
                        is MainComponent.Child.Collaboration -> CollaborationContent(component = instance.component)
                        is MainComponent.Child.VersionHistory -> VersionHistoryContent(component = instance.component)
                        is MainComponent.Child.ChangePassword -> ChangePasswordContent(component = instance.component)
                        is MainComponent.Child.Security -> SecurityContent(component = instance.component)
                        is MainComponent.Child.Licenses -> LicensesContent(component = instance.component)
                    }
                }
            }

            // Bottom-right upload progress banner (Google Drive style)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd,
            ) {
                UploadBanner(
                    uploadItems = uploadItems,
                    isMinimized = uploadMinimized,
                    onSetMinimized = uploadManager::setMinimized,
                    onDismiss = uploadManager::dismissCompleted,
                    onCancelItem = uploadManager::cancelUpload,
                )
            }
        }
    }
}
