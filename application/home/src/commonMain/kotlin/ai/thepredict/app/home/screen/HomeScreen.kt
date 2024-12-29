package ai.thepredict.app.home.screen

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen

internal class HomeScreen : Screen {
    @Composable
    override fun Content() {
        Text("Home")
    }
}