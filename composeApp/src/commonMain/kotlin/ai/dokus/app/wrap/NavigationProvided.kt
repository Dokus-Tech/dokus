package ai.dokus.app.app.wrap

import ai.dokus.app.app.navigation.NavigationGraph
import androidx.compose.runtime.Composable

@Composable
fun NavigationProvided(content: @Composable () -> Unit) {
    NavigationGraph()
    content()
}