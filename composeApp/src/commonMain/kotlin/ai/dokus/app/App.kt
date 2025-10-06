package ai.dokus.app

import ai.dokus.foundation.platform.persistence
import ai.dokus.app.wrap.Bootstrapped
import ai.dokus.app.wrap.NavigationProvided
import ai.dokus.foundation.ui.Themed
import ai.dokus.app.repository.extensions.user
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