package ai.thepredict.app

import ai.thepredict.app.platform.persistence
import ai.thepredict.app.wrap.Bootstrapped
import ai.thepredict.app.wrap.NavigationProvided
import ai.thepredict.ui.Themed
import ai.thepredict.repository.extensions.user
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import org.jetbrains.compose.ui.tooling.preview.Preview

private val clearUser = false

@Preview
@Composable
fun App() {
    Bootstrapped {
        Themed {
            if (clearUser) {
                LaunchedEffect("clearUser") {
                    persistence.user = null
                    persistence.jwtToken = null
                    persistence.selectedWorkspace = null
                }
            }
            NavigationProvided {
                // Navigation content is handled by NavigationGraph
            }
        }
    }
}