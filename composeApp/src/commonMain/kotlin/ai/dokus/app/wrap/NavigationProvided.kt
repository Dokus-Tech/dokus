package ai.dokus.app.wrap

import androidx.compose.runtime.Composable

@Composable
fun NavigationProvided(content: @Composable () -> Unit) {
    content()
}