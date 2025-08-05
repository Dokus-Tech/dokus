package ai.thepredict.app.profile

import ai.thepredict.app.profile.screen.ProfileScreen
import ai.thepredict.app.navigation.HomeTabsNavigation
import cafe.adriel.voyager.core.registry.screenModule

val profileScreensModule = screenModule {
    register<HomeTabsNavigation.Profile> {
        ProfileScreen()
    }
}