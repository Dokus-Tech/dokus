package ai.dokus.app.app.home.splash

import ai.dokus.app.app.navigation.AppNavigator
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(navigator: AppNavigator) {
    val scope = rememberCoroutineScope()
    val viewModel = remember { SplashScreenViewModel() }

    val handleEffect = { effect: SplashScreenViewModel.Effect ->
        when (effect) {
            is SplashScreenViewModel.Effect.Idle -> {}

            is SplashScreenViewModel.Effect.NavigateToLogin -> {
                navigator.navigateToLogin()
            }

            is SplashScreenViewModel.Effect.NavigateToWorkspacesOverview -> {
                navigator.navigateToWorkspacesList()
            }

            is SplashScreenViewModel.Effect.NavigateHome -> {
                navigator.navigateToHome()
            }
        }
    }

    LaunchedEffect("splash-screen") {
        scope.launch { viewModel.state.collect(handleEffect) }
        viewModel.checkOnboarding()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AdaptiveCircularProgressIndicator()
        Text("Loading")
    }
}