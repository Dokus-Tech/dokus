package ai.thepredict.app.navigation

import cafe.adriel.voyager.core.registry.ScreenProvider

sealed interface HomeNavigation : ScreenProvider {
    data object HomeScreen : HomeNavigation
}