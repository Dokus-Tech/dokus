package ai.dokus.app

import ai.dokus.app.core.navigationProviders
import ai.dokus.app.local.AppModulesInitializer
import ai.dokus.app.local.AppModulesProvided
import ai.dokus.app.local.KoinProvided
import ai.dokus.foundation.design.local.ScreenSizeProvided
import ai.dokus.foundation.design.style.Themed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import ai.dokus.app.navigation.secondary.DualPanelNavigationContainer

@Composable
fun App(
    onNavHostReady: suspend (NavController) -> Unit = {}
) {
    val modules = remember { appModules }
    val navigationProviders = remember(modules) { modules.navigationProviders }
    val diModules = remember(modules) { modules.diModules }

    AppModulesProvided(modules) {
        KoinProvided(diModules) {
            AppModulesInitializer(modules) {
                Themed {
                    ScreenSizeProvided {
                        DualPanelNavigationContainer(
                            navigationProviders = navigationProviders,
                            onPrimaryNavHostReady = onNavHostReady
                        )
                    }
                }
            }
        }
    }
}