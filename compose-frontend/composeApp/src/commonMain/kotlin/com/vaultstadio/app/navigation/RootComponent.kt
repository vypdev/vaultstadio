package com.vaultstadio.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.arkivanov.decompose.value.Value
import com.vaultstadio.app.data.repository.AuthRepository
import com.vaultstadio.app.feature.auth.AuthComponent
import com.vaultstadio.app.feature.auth.DefaultAuthComponent
import com.vaultstadio.app.feature.main.DefaultMainComponent
import com.vaultstadio.app.feature.main.MainComponent
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Root navigation component that handles auth/main split.
 */
interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    sealed class Child {
        data class Auth(val component: AuthComponent) : Child()
        data class Main(val component: MainComponent) : Child()
    }
}

/**
 * Default implementation of RootComponent.
 *
 * Uses Koin for dependency injection. Child components also use Koin.
 * [initialPath] is used to open a specific screen when the app loads (e.g. from URL on web).
 */
class DefaultRootComponent(
    componentContext: ComponentContext,
    private val initialPath: String = "/",
) : RootComponent, ComponentContext by componentContext, KoinComponent {

    // Only inject what we need at this level
    private val authRepository: AuthRepository by inject()

    private val navigation = StackNavigation<Config>()

    private val initialConfig: Config
        get() = when {
            RoutePaths.isAuthPath(initialPath) -> Config.Auth
            authRepository.isLoggedIn() -> Config.Main
            else -> Config.Auth
        }

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = initialConfig,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    private fun createChild(config: Config, context: ComponentContext): RootComponent.Child =
        when (config) {
            is Config.Auth -> RootComponent.Child.Auth(
                DefaultAuthComponent(
                    componentContext = context,
                    onAuthSuccess = ::onAuthSuccess,
                ),
            )
            is Config.Main -> RootComponent.Child.Main(
                DefaultMainComponent(
                    componentContext = context,
                    onLogout = ::onLogout,
                    initialPath = initialPath,
                ),
            )
        }

    private fun onAuthSuccess() {
        navigation.replaceCurrent(Config.Main)
    }

    private fun onLogout() {
        navigation.replaceCurrent(Config.Auth)
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Auth : Config

        @Serializable
        data object Main : Config
    }
}
