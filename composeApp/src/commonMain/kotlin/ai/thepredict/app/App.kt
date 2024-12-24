package ai.thepredict.app

import ai.thepredict.app.wrap.Bootstrapped
import ai.thepredict.app.wrap.NavigationProvided
import ai.thepredict.app.wrap.Themed
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.CurrentScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun App() {
    Bootstrapped {
        Themed {
            NavigationProvided {
                Scaffold {
                    CurrentScreen()
                }
            }
        }
    }
}