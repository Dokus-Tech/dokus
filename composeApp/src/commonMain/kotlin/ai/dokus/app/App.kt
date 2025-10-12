package ai.dokus.app

import ai.dokus.app.wrap.Bootstrapped
import ai.dokus.app.wrap.NavigationProvided
import ai.dokus.foundation.ui.Themed
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun App() {
    Bootstrapped {
        Themed {
            NavigationProvided {
                // Navigation content is handled by NavigationGraph
            }
        }
    }
}