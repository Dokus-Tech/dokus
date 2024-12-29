package ai.thepredict.app.wrap

import ai.thepredict.app.core.configureDi
import ai.thepredict.app.home.homeDiModule
import ai.thepredict.app.onboarding.onboardingDiModule
import ai.thepredict.repository.repositoryDiModule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun Bootstrapped(content: @Composable () -> Unit) {
    LaunchedEffect("app-bootstrap") {
        configureDi(repositoryDiModule, onboardingDiModule, homeDiModule)
    }

    content()
}