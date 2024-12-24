package ai.thepredict.app.wrap

import ai.thepredict.app.onboarding.onboardingScreensModule
import ai.thepredict.app.onboarding.splash.SplashScreen
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.registry.ScreenRegistry
import cafe.adriel.voyager.jetpack.ProvideNavigatorLifecycleKMPSupport
import cafe.adriel.voyager.navigator.Navigator

@OptIn(ExperimentalVoyagerApi::class)
@Composable
fun NavigationProvided(content: @Composable () -> Unit) {
    ScreenRegistry {
        onboardingScreensModule()
    }

    ProvideNavigatorLifecycleKMPSupport {
        Navigator(SplashScreen()) {
            content()
        }
    }
}