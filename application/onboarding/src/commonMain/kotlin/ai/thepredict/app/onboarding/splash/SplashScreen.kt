package ai.thepredict.app.onboarding.splash

import ai.thepredict.app.navigation.HomeNavigation
import ai.thepredict.app.navigation.OnboardingNavigation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator

class SplashScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = rememberScreenModel { SplashScreenViewModel() }
        val data = viewModel.state.collectAsState()

        val loginScreen = rememberScreen(OnboardingNavigation.Authorization.LoginScreen)
        val homeScreen = rememberScreen(HomeNavigation.HomeScreen)
        val workspacesOverview = rememberScreen(OnboardingNavigation.Workspaces.All)

        LaunchedEffect("splash-screen") {
            viewModel.checkOnboarding()
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AdaptiveCircularProgressIndicator()
            Text("Loading")
        }

        when (data.value) {
            is SplashScreenViewModel.Effect.Idle -> {}

            is SplashScreenViewModel.Effect.NavigateToLogin -> {
                navigator.replace(loginScreen)
            }

            is SplashScreenViewModel.Effect.NavigateToWorkspacesOverview -> {
                navigator.replace(workspacesOverview)
            }

            is SplashScreenViewModel.Effect.NavigateHome -> {
                navigator.replace(homeScreen)
            }
        }
    }
}