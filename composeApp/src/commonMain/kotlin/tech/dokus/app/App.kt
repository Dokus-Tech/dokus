package tech.dokus.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.koin.compose.koinInject
import tech.dokus.app.local.AppModulesInitializer
import tech.dokus.app.local.AppModulesProvided
import tech.dokus.app.local.KoinProvided
import tech.dokus.app.navigation.DokusNavHost
import tech.dokus.app.navigation.local.RootNavControllerProvided
import tech.dokus.foundation.app.AppDataInitializer
import tech.dokus.foundation.app.navigationProviders
import tech.dokus.foundation.app.network.ServerConnectionMonitor
import tech.dokus.foundation.app.network.ServerConnectionProvided
import tech.dokus.foundation.aura.local.ScreenSizeProvided
import tech.dokus.foundation.aura.local.ThemeManagerProvided
import tech.dokus.foundation.aura.style.ThemeManager
import tech.dokus.foundation.aura.style.Themed
import tech.dokus.navigation.local.NavControllerProvided

@Composable
fun App(
    onNavHostReady: suspend (NavController) -> Unit = {}
) {
    val modules = remember { appModules }
    val navigationProviders = remember(modules) { modules.navigationProviders }
    val diModules = remember(modules) { modules.diModules }
    val navController = rememberNavController()

    AppModulesProvided(modules) {
        KoinProvided(diModules) {
            val themeManager = koinInject<ThemeManager>()
            val serverConnectionMonitor = koinInject<ServerConnectionMonitor>()
            val appDataInitializer = koinInject<AppDataInitializer>()

            // Provide server connection state to entire app
            // Note: Connection monitoring is now event-driven - the monitor is notified
            // automatically when HTTP requests succeed or fail with network errors
            ServerConnectionProvided(serverConnectionMonitor) {
                ThemeManagerProvided(themeManager) {
                    Themed {
                        AppModulesInitializer(appDataInitializer) {
                            ScreenSizeProvided {
                                RootNavControllerProvided(navController) {
                                    NavControllerProvided(navController) {
                                        DokusNavHost(
                                            navController = navController,
                                            navigationProvider = navigationProviders,
                                            onNavHostReady = onNavHostReady
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
