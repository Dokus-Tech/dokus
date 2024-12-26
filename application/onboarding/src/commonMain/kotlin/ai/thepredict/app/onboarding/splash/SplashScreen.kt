package ai.thepredict.app.onboarding.splash

import ai.thepredict.app.navigation.OnboardingNavigation
import ai.thepredict.ui.PButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class SplashScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val loginScreen = rememberScreen(OnboardingNavigation.Authorization.LoginScreen)
        val workspaceSelectionScreen =
            rememberScreen(OnboardingNavigation.Workspaces.WorkspacesSelectionScreen)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PButton("Login") {
                navigator.replace(loginScreen)
            }
            PButton("Workspace selection") {
                navigator.replace(workspaceSelectionScreen)
            }
        }
    }
}