package ai.thepredict.app.home

import ai.thepredict.app.home.screen.HomeScreen
import ai.thepredict.app.home.splash.SplashScreen
import ai.thepredict.app.navigation.CoreNavigation
import cafe.adriel.voyager.core.registry.screenModule

val homeScreensModule = screenModule {
    register<CoreNavigation.Splash> {
        SplashScreen()
    }
    register<CoreNavigation.Core> {
        HomeScreen()
    }
}