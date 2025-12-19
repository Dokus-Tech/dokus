package tech.dokus.app

import tech.dokus.foundation.app.navigationProviders
import tech.dokus.app.local.AppModulesInitializer
import tech.dokus.app.local.AppModulesProvided
import tech.dokus.app.local.KoinProvided
import tech.dokus.app.navigation.DokusNavHost
import ai.dokus.foundation.design.local.ScreenSizeProvided
import ai.dokus.foundation.design.local.ThemeManagerProvided
import ai.dokus.foundation.design.style.ThemeManager
import ai.dokus.foundation.design.style.Themed
import ai.dokus.foundation.navigation.local.NavControllerProvided
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.koin.compose.koinInject

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
            ThemeManagerProvided(themeManager) {
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
}