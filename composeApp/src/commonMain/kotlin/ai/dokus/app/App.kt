package ai.dokus.app

import ai.dokus.app.core.navigationProviders
import ai.dokus.app.local.AppModulesInitializer
import ai.dokus.app.local.AppModulesProvided
import ai.dokus.app.local.KoinProvided
import ai.dokus.app.navigation.DokusNavHost
import ai.dokus.foundation.design.local.ScreenSizeProvided
import ai.dokus.foundation.design.style.Themed
import ai.dokus.foundation.navigation.local.NavControllerProvided
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

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
            Themed {
                AppModulesInitializer(modules) {
                    ScreenSizeProvided {
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