package com.vaultstadio.app.feature.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.vaultstadio.app.data.network.TokenStorage
import com.vaultstadio.app.domain.auth.AuthRepository
import com.vaultstadio.app.domain.auth.model.User
import com.vaultstadio.app.domain.auth.model.UserRole
import com.vaultstadio.app.feature.activity.ActivityComponent
import com.vaultstadio.app.feature.activity.DefaultActivityComponent
import com.vaultstadio.app.feature.admin.AdminComponent
import com.vaultstadio.app.feature.admin.DefaultAdminComponent
import com.vaultstadio.app.feature.ai.AIComponent
import com.vaultstadio.app.feature.ai.DefaultAIComponent
import com.vaultstadio.app.feature.changepassword.ChangePasswordComponent
import com.vaultstadio.app.feature.changepassword.DefaultChangePasswordComponent
import com.vaultstadio.app.feature.collaboration.CollaborationComponent
import com.vaultstadio.app.feature.collaboration.DefaultCollaborationComponent
import com.vaultstadio.app.feature.federation.DefaultFederationComponent
import com.vaultstadio.app.feature.federation.FederationComponent
import com.vaultstadio.app.feature.files.DefaultFilesComponent
import com.vaultstadio.app.feature.files.FilesComponent
import com.vaultstadio.app.feature.licenses.DefaultLicensesComponent
import com.vaultstadio.app.feature.licenses.LicensesComponent
import com.vaultstadio.app.feature.plugins.DefaultPluginsComponent
import com.vaultstadio.app.feature.plugins.PluginsComponent
import com.vaultstadio.app.feature.profile.DefaultProfileComponent
import com.vaultstadio.app.feature.profile.ProfileComponent
import com.vaultstadio.app.feature.security.DefaultSecurityComponent
import com.vaultstadio.app.feature.security.SecurityComponent
import com.vaultstadio.app.feature.settings.DefaultSettingsComponent
import com.vaultstadio.app.feature.settings.SettingsComponent
import com.vaultstadio.app.feature.sharedwithme.DefaultSharedWithMeComponent
import com.vaultstadio.app.feature.sharedwithme.SharedWithMeComponent
import com.vaultstadio.app.feature.shares.DefaultSharesComponent
import com.vaultstadio.app.feature.shares.SharesComponent
import com.vaultstadio.app.feature.sync.DefaultSyncComponent
import com.vaultstadio.app.feature.sync.SyncComponent
import com.vaultstadio.app.feature.versionhistory.DefaultVersionHistoryComponent
import com.vaultstadio.app.feature.versionhistory.VersionHistoryComponent
import com.vaultstadio.app.navigation.MainDestination
import com.vaultstadio.app.navigation.RoutePaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Main component that contains all authenticated screens.
 */
interface MainComponent {
    val stack: Value<ChildStack<*, Child>>
    val currentUser: User?
    val isAdmin: Boolean

    fun navigateTo(destination: MainDestination)
    fun onBack()
    fun logout()

    sealed class Child {
        data class Files(val component: FilesComponent, val mode: FilesMode) : Child()
        data class Shares(val component: SharesComponent) : Child()
        data class SharedWithMe(val component: SharedWithMeComponent) : Child()
        data class Settings(val component: SettingsComponent) : Child()
        data class Profile(val component: ProfileComponent) : Child()
        data class Admin(val component: AdminComponent) : Child()
        data class Activity(val component: ActivityComponent) : Child()
        data class Plugins(val component: PluginsComponent) : Child()
        data class AI(val component: AIComponent) : Child()
        data class Sync(val component: SyncComponent) : Child()
        data class Federation(val component: FederationComponent) : Child()
        data class Collaboration(val component: CollaborationComponent) : Child()
        data class VersionHistory(val component: VersionHistoryComponent) : Child()
        data class ChangePassword(val component: ChangePasswordComponent) : Child()
        data class Security(val component: SecurityComponent) : Child()
        data class Licenses(val component: LicensesComponent) : Child()
    }

    enum class FilesMode { ALL, RECENT, STARRED, TRASH }
}

/**
 * Default implementation of MainComponent.
 *
 * Uses Koin for dependency injection - child components also use Koin.
 */
class DefaultMainComponent(
    componentContext: ComponentContext,
    private val onLogout: () -> Unit,
    private val initialPath: String = "/",
) : MainComponent, ComponentContext by componentContext, KoinComponent {

    // Inject only what we need directly
    private val authRepository: AuthRepository by inject()
    private val tokenStorage: TokenStorage by inject()

    private val scope = coroutineScope(Dispatchers.Main + SupervisorJob())
    private val navigation = StackNavigation<Config>()

    // Token provider for components that need direct API access
    private val tokenProvider: () -> String? = { tokenStorage.getAccessToken() }

    override val currentUser: User? get() = authRepository.currentUser
    override val isAdmin: Boolean get() = currentUser?.role == UserRole.ADMIN

    init {
        // Restore current user from token when app loads (e.g. after page refresh on web)
        scope.launch {
            if (tokenStorage.getAccessToken() != null && authRepository.currentUser == null) {
                authRepository.refreshCurrentUser()
            }
        }
    }

    private fun configFromDestination(
        destination: MainDestination,
        pathSegments: List<String> = emptyList(),
    ): Config = when (destination) {
        MainDestination.FILES -> Config.Files(MainComponent.FilesMode.ALL, pathSegments)
        MainDestination.RECENT -> Config.Files(MainComponent.FilesMode.RECENT, pathSegments)
        MainDestination.STARRED -> Config.Files(MainComponent.FilesMode.STARRED, pathSegments)
        MainDestination.TRASH -> Config.Files(MainComponent.FilesMode.TRASH, pathSegments)
        MainDestination.SHARED -> Config.Shares
        MainDestination.SHARED_WITH_ME -> Config.SharedWithMe
        MainDestination.SETTINGS -> Config.Settings
        MainDestination.PROFILE -> Config.Profile
        MainDestination.ADMIN -> Config.Admin
        MainDestination.ACTIVITY -> Config.Activity
        MainDestination.PLUGINS -> Config.Plugins
        MainDestination.AI -> Config.AI
        MainDestination.SYNC -> Config.Sync
        MainDestination.FEDERATION -> Config.Federation
        MainDestination.COLLABORATION -> Config.Collaboration
        MainDestination.VERSION_HISTORY -> Config.VersionHistory
        MainDestination.CHANGE_PASSWORD -> Config.ChangePassword
        MainDestination.SECURITY -> Config.Security
        MainDestination.LICENSES -> Config.Licenses
    }

    private val initialStack: List<MainDestination> =
        RoutePaths.getStack(initialPath).ifEmpty { listOf(MainDestination.FILES) }

    private val initialPathSegments: List<String> =
        RoutePaths.parseRoute(initialPath)?.pathSegments() ?: emptyList()

    override val stack: Value<ChildStack<*, MainComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = configFromDestination(
                initialStack.first(),
                pathSegments = if (initialStack.first() == MainDestination.FILES) initialPathSegments else emptyList(),
            ),
            handleBackButton = true,
            childFactory = ::createChild,
        )

    init {
        // Build stack from path (go_router style): push remaining destinations so Back works
        initialStack.drop(1).forEach { navigation.push(configFromDestination(it)) }
    }

    @OptIn(com.arkivanov.decompose.DelicateDecomposeApi::class)
    override fun navigateTo(destination: MainDestination) {
        val config = configFromDestination(destination)
        val current = stack.value
        // backStack is already bottom-to-top; active is the top
        val configsFromBottomToTop =
            current.backStack.map { it.configuration } + current.active.configuration
        val index = configsFromBottomToTop.indexOf(config)
        if (index >= 0) {
            repeat(configsFromBottomToTop.size - 1 - index) { navigation.pop() }
        } else {
            navigation.push(config)
        }
    }

    override fun onBack() {
        navigation.pop()
    }

    override fun logout() {
        scope.launch {
            authRepository.logout()
            onLogout()
        }
    }

    private fun goBack() {
        navigation.pop()
    }

    private fun createChild(config: Config, context: ComponentContext): MainComponent.Child =
        when (config) {
            is Config.Files -> MainComponent.Child.Files(
                component = DefaultFilesComponent(
                    componentContext = context,
                    mode = config.mode,
                    pathSegments = config.pathSegments,
                ),
                mode = config.mode,
            )
            is Config.Shares -> MainComponent.Child.Shares(
                DefaultSharesComponent(context),
            )
            is Config.SharedWithMe -> MainComponent.Child.SharedWithMe(
                DefaultSharedWithMeComponent(context, ::goBack),
            )
            is Config.Settings -> MainComponent.Child.Settings(
                DefaultSettingsComponent(
                    componentContext = context,
                    onNavigateBack = ::goBack,
                    onLogoutAction = ::logout,
                    onNavigateToProfile = { navigateTo(MainDestination.PROFILE) },
                    onNavigateToChangePassword = { navigateTo(MainDestination.CHANGE_PASSWORD) },
                    onNavigateToLicenses = { navigateTo(MainDestination.LICENSES) },
                    initialThemeMode = com.vaultstadio.app.ui.theme.ThemeSettings.themeMode,
                    onThemeModeChangeCallback = { mode: com.vaultstadio.app.core.resources.ThemeMode ->
                        com.vaultstadio.app.ui.theme.ThemeSettings.themeMode = mode
                    },
                    initialLanguage = com.vaultstadio.app.core.resources.Strings.currentLanguage,
                    onLanguageChangeCallback = { lang: com.vaultstadio.app.core.resources.Language ->
                        com.vaultstadio.app.core.resources.Strings.currentLanguage = lang
                        try {
                            com.vaultstadio.app.platform.PlatformStorage.setString(
                                com.vaultstadio.app.platform.StorageKeys.LANGUAGE,
                                lang.code,
                            )
                        } catch (_: Exception) { /* ignore */ }
                    },
                ),
            )
            is Config.Profile -> MainComponent.Child.Profile(
                DefaultProfileComponent(
                    componentContext = context,
                    onNavigateBack = ::goBack,
                    onNavigateToChangePassword = { navigateTo(MainDestination.CHANGE_PASSWORD) },
                    onNavigateToSecurity = { navigateTo(MainDestination.SECURITY) },
                    onExportData = { /* Will be handled in ProfileViewModel */ },
                ),
            )
            is Config.Admin -> MainComponent.Child.Admin(
                DefaultAdminComponent(context, ::goBack),
            )
            is Config.Activity -> MainComponent.Child.Activity(
                DefaultActivityComponent(context),
            )
            is Config.Plugins -> MainComponent.Child.Plugins(
                DefaultPluginsComponent(context, ::goBack),
            )
            is Config.AI -> MainComponent.Child.AI(
                DefaultAIComponent(context, isAdmin = currentUser?.role == UserRole.ADMIN),
            )
            is Config.Sync -> MainComponent.Child.Sync(
                DefaultSyncComponent(context),
            )
            is Config.Federation -> MainComponent.Child.Federation(
                DefaultFederationComponent(context),
            )
            is Config.Collaboration -> MainComponent.Child.Collaboration(
                DefaultCollaborationComponent(context, "", "", ::goBack),
            )
            is Config.VersionHistory -> MainComponent.Child.VersionHistory(
                DefaultVersionHistoryComponent(context, "", "", ::goBack),
            )
            is Config.ChangePassword -> MainComponent.Child.ChangePassword(
                DefaultChangePasswordComponent(context, ::goBack),
            )
            is Config.Security -> MainComponent.Child.Security(
                DefaultSecurityComponent(context, ::goBack),
            )
            is Config.Licenses -> MainComponent.Child.Licenses(
                DefaultLicensesComponent(context, ::goBack),
            )
        }

    @Serializable
    private sealed interface Config {
        @Serializable
        data class Files(val mode: MainComponent.FilesMode, val pathSegments: List<String> = emptyList()) : Config

        @Serializable
        data object Shares : Config

        @Serializable
        data object SharedWithMe : Config

        @Serializable
        data object Settings : Config

        @Serializable
        data object Profile : Config

        @Serializable
        data object Admin : Config

        @Serializable
        data object Activity : Config

        @Serializable
        data object Plugins : Config

        @Serializable
        data object AI : Config

        @Serializable
        data object Sync : Config

        @Serializable
        data object Federation : Config

        @Serializable
        data object Collaboration : Config

        @Serializable
        data object VersionHistory : Config

        @Serializable
        data object ChangePassword : Config

        @Serializable
        data object Security : Config

        @Serializable
        data object Licenses : Config
    }
}
