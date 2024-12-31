package ai.thepredict.app.home

import ai.thepredict.app.home.screen.HomeScreen
import ai.thepredict.app.home.splash.SplashScreen
import ai.thepredict.app.navigation.HomeNavigation
import cafe.adriel.voyager.core.registry.screenModule

val homeScreensModule = screenModule {
    register<HomeNavigation.SplashScreen> {
        SplashScreen()
    }
    register<HomeNavigation.HomeScreen> {
        HomeScreen()
    }
}