package ai.thepredict.app.home.screen

import ai.thepredict.ui.navigation.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen

internal class HomeScreen : Screen {
    @Composable
    override fun Content() {
        Scaffold(
            bottomBar = {
                NavigationBar()
            }
        ) {
            Text("Home")
        }
    }
}