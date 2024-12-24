package ai.thepredict.app.onboarding.splash

import ai.thepredict.app.navigation.OnboardingNavigation
import ai.thepredict.ui.PButton
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class SplashScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val loginScreen = rememberScreen(OnboardingNavigation.Authorization.LoginScreen)

        PButton("Go to login") {
            navigator.replace(loginScreen)
        }
    }
}