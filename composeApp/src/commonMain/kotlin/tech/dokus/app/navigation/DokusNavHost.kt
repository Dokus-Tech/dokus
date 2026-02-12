package tech.dokus.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavUri
import androidx.navigation.compose.NavHost
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import tech.dokus.domain.asbtractions.AuthManager
import tech.dokus.domain.model.auth.AuthEvent
import tech.dokus.domain.model.common.DeepLinks
import tech.dokus.domain.model.common.KnownDeepLinks
import tech.dokus.app.share.ExternalShareImportHandler
import tech.dokus.app.share.PlatformShareImportBridge
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.isLarge
import tech.dokus.foundation.platform.activePlatform
import tech.dokus.foundation.platform.isDesktop
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.animation.TransitionsProvider
import tech.dokus.navigation.destinations.AppDestination
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.CoreDestination
import tech.dokus.navigation.destinations.NavigationDestination
import tech.dokus.navigation.navigateTo
import tech.dokus.navigation.replace
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
            ExternalShareImportHandler.pendingState.collect { pendingFiles ->
                if (!pendingFiles.isNullOrEmpty()) {
                    navController.replace(AppDestination.ShareImport)
                }
            }
        }

        launch {
            ExternalUriHandler.deeplinkState.collect { deepLink ->
                if (deepLink != null) {
                    println("Collecting deeplink state: $deepLink")
                    delay(0.5.seconds)

                    if (deepLink.path.startsWith(KnownDeepLinks.ShareImport.path.path)) {
                        val batchId = DeepLinks.extractShareImportBatchId(deepLink)
                        PlatformShareImportBridge.consumeBatch(batchId)
                            .onSuccess { files ->
                                if (files.isNotEmpty()) {
                                    ExternalShareImportHandler.onNewSharedFiles(files)
                                } else {
                                    println("[DokusNavHost] No share batch payload found for id=$batchId")
                                }
                            }
                            .onFailure { error ->
                                println("[DokusNavHost] Failed to consume share batch: ${error.message}")
                            }
                        return@collect
                    }

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
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .then(
                if (activePlatform.isDesktop) {
                    Modifier.padding(top = 12.dp)
                } else {
                    Modifier
                }
            ),
    ) {
        navigationProvider.forEach { provider ->
            with(provider) {
                registerGraph()
            }
        }
    }
}
