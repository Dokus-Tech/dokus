package ai.thepredict.app.wrap

import ai.thepredict.app.navigation.NavigationGraph
import androidx.compose.runtime.Composable

@Composable
fun NavigationProvided(content: @Composable () -> Unit) {
    NavigationGraph()
    content()
}