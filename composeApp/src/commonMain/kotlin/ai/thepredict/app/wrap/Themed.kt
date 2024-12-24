package ai.thepredict.app.wrap

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun Themed(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}