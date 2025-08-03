package ai.thepredict.app.home.screen

import ai.thepredict.ui.navigation.NavigationBar
import ai.thepredict.ui.navigation.NavigationItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen

internal class HomeScreen : Screen {
    @Composable
    override fun Content() {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    navigationItems = NavigationItem.all,
                    fabItem = NavigationItem.AddDocuments,
                    selectedIndex = 0,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) { clickedItem ->

                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier.padding(innerPadding)
            ) {
                Text("Home")
            }
        }
    }
}