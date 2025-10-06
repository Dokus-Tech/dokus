package ai.dokus.app.app.simulations.screen

import ai.dokus.app.app.navigation.AppNavigator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SimulationScreen(navigator: AppNavigator) {
    Scaffold {
        Box(modifier = Modifier.padding(it)) {
            Text("Simulation", modifier = Modifier.align(Alignment.Center))
        }
    }
}