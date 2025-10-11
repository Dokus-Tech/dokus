package ai.dokus.app.wrap

import ai.dokus.foundation.navigation.NavigationGraph
import androidx.compose.runtime.Composable

@Composable
fun NavigationProvided(content: @Composable () -> Unit) {
    NavigationGraph()
    content()
}