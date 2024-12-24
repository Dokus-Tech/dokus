package ai.thepredict.app.wrap

import ai.thepredict.onboarding.configureDi
import ai.thepredict.onboarding.onboardingDiModule
import ai.thepredict.repository.repositoryDiModule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun Bootstrapped(content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()

    LaunchedEffect("app-bootstrap") {
        configureDi(repositoryDiModule, onboardingDiModule)
    }

    Themed {
        content()
    }
}