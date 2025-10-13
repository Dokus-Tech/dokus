package ai.dokus.app.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator

@Composable
fun SplashScreen() {
    val scope = rememberCoroutineScope()
//    val viewModel = remember { SplashScreenViewModel() }

    LaunchedEffect("splash-screen") {
//        scope.launch { viewModel.state.collect(handleEffect) }
//        viewModel.checkOnboarding()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AdaptiveCircularProgressIndicator()
        Text("Loading")
    }
}