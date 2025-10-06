package ai.dokus.app.wrap

import ai.dokus.app.navigation.NavigationGraph
import androidx.compose.runtime.Composable

@Composable
fun NavigationProvided(content: @Composable () -> Unit) {
    NavigationGraph()
    content()
}