package ai.thepredict.app.simulations.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen

internal class SimulationScreen : Screen {
    @Composable
    override fun Content() {
        Scaffold {
            Box(modifier = Modifier.padding(it)) {
                Text("Simulation", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}