package ai.thepredict.app.home.screen

import ai.thepredict.ui.navigation.NavigationBar
import ai.thepredict.ui.navigation.NavigationItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen

internal class HomeScreen : Screen {
    @Composable
    override fun Content() {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    navigationItems = NavigationItem.all,
                    selectedIndex = 0
                )
            }
        ) {
            Text("Home")
        }
    }
}