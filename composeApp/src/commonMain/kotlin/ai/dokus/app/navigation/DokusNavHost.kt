package ai.dokus.app.navigation

import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.design.local.isLarge
import ai.dokus.foundation.domain.asbtractions.AuthManager
import ai.dokus.foundation.domain.model.AuthEvent
import ai.dokus.foundation.domain.model.common.DeepLinks
import ai.dokus.foundation.domain.model.common.KnownDeepLinks
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.animation.TransitionsProvider
import ai.dokus.foundation.navigation.destinations.AuthDestination
import ai.dokus.foundation.navigation.destinations.CoreDestination
import ai.dokus.foundation.navigation.destinations.NavigationDestination
import ai.dokus.foundation.navigation.navigateTo
import ai.dokus.foundation.navigation.replace
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavUri
import androidx.navigation.compose.NavHost
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.seconds

@Composable
fun DokusNavHost(
    navController: NavHostController,
    navigationProvider: List<NavigationProvider>,
    onNavHostReady: suspend (NavController) -> Unit = {},
    authManager: AuthManager = koinInject(),
) {
    // Notify that NavHost is ready
    LaunchedEffect(navController) {
        onNavHostReady(navController)
    }

    LaunchedEffect(navController) {
        launch {
            ExternalUriHandler.deeplinkState.collect { deepLink ->
                if (deepLink != null) {
                    println("Collecting deeplink state: $deepLink")
                    delay(0.5.seconds)

                    // Handle server connect deep links specially
                    if (deepLink.path.startsWith(KnownDeepLinks.ServerConnect.path.path)) {
                        val params = DeepLinks.extractServerConnect(deepLink)
                        if (params != null) {
                            val (host, port, protocol) = params
                            navController.navigateTo(
                                AuthDestination.ServerConnection(
                                    host = host,
                                    port = port,
                                    protocol = protocol
                                )
                            )
                        } else {
                            // Invalid params, navigate to server connection without pre-fill
                            navController.navigateTo(AuthDestination.ServerConnection())
                        }
                    } else {
                        // Handle other deep links normally
                        navController.navigateTo(NavUri(deepLink.withAppScheme))
                    }
                }
            }
        }
    }

    // Observe authentication events globally
    LaunchedEffect(authManager) {
        authManager.authenticationEvents.collectLatest { event ->
            when (event) {
                is AuthEvent.ForceLogout -> {
                    // Force navigation to login screen and clear backstack
                    navController.replace(AuthDestination.Login)
                }

                is AuthEvent.UserLogout -> {
                    // Navigate to login screen
                    navController.replace(AuthDestination.Login)
                }

                is AuthEvent.LoginSuccess -> {}
            }
        }
    }

    val largeScreen = LocalScreenSize.isLarge
    val transitionsProvider: TransitionsProvider by remember(largeScreen) {
        derivedStateOf(policy = referentialEqualityPolicy()) {
            TransitionsProvider.forRoot(largeScreen)
        }
    }
    NavHost(
        navController = navController,
        startDestination = CoreDestination.Splash as NavigationDestination,
        enterTransition = { with(transitionsProvider) { enterTransition } },
        exitTransition = { with(transitionsProvider) { exitTransition } },
        popEnterTransition = { with(transitionsProvider) { popEnterTransition } },
        popExitTransition = { with(transitionsProvider) { popExitTransition } },
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
    ) {
        navigationProvider.forEach { provider ->
            with(provider) {
                registerGraph()
            }
        }
    }
}