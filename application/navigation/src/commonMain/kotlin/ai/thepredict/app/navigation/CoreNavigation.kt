package ai.thepredict.app.navigation

import cafe.adriel.voyager.core.registry.ScreenProvider

sealed interface CoreNavigation : ScreenProvider {
    data object Splash : CoreNavigation
    data object Core : CoreNavigation
}