package ai.thepredict.app

import ai.thepredict.app.wrap.Bootstrapped
import ai.thepredict.onboarding.configureDi
import ai.thepredict.onboarding.onboardingDiModule
import ai.thepredict.platform.Greeting
import ai.thepredict.platform.getPlatform
import ai.thepredict.repository.repositoryDiModule
import ai.thepredict.ui.PButton
import ai.thepredict.ui.Title
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.kodein.di.instance
import thepredict.composeapp.generated.resources.Res
import thepredict.composeapp.generated.resources.compose_multiplatform

@Preview
@Composable
fun App() {
    Bootstrapped {
        var showContent by remember { mutableStateOf(false) }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            PButton(text = "Click me", icon = Icons.Filled.Call) {
                showContent = !showContent
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Title("Compose: $greeting")
                    Title("Platform: ${getPlatform().name}")
                }
            }
        }
    }
}